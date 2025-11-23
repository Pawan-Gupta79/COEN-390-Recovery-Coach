package com.team11.smartgym.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.team11.smartgym.R;
import com.team11.smartgym.shared.Bus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

public class BleService extends Service {

    public static final String ACTION_START = "START";
    public static final String ACTION_CONNECT = "CONNECT";
    public static final String EXTRA_DEVICE = "DEVICE_ADDR";

    private static final String PREF_LAST = "last_mac";

    // Standard Heart Rate service/characteristic.
    private static final UUID UUID_SERVICE_HR =
            UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB");
    private static final UUID UUID_CHAR_HR =
            UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private SharedPreferences prefs;
    private boolean reconnecting = false;

    // Simple moving window for smoothing.
    private final Queue<Integer> smooth = new ArrayDeque<>();

    // Last known sensor-contact state (from HR flags).
    // Not yet exposed in UI, but parsed and tracked for DS-04.1.
    private volatile boolean lastContactDetected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bm.getAdapter();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        startForegroundWithNotif("Idle");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CONNECT.equals(intent.getAction())) {
            String mac = intent.getStringExtra(EXTRA_DEVICE);
            prefs.edit().putString(PREF_LAST, mac).apply();
            connect(mac);
        } else if (intent != null && ACTION_START.equals(intent.getAction())) {
            String last = prefs.getString(PREF_LAST, null);
            if (last != null && prefs.getBoolean("auto_reconnect", true)) {
                connect(last);
            }
        }
        return START_STICKY;
    }

    private void startForegroundWithNotif(String state) {
        String chId = "ble";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    chId,
                    "BLE",
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, chId)
                .setContentTitle("Smart Gym")
                .setContentText("State: " + state)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(11, n);
    }

    private void connect(String mac) {
        if (mac == null) {
            Bus.sendError(this, "No device selected");
            return;
        }
        BluetoothDevice dev = adapter.getRemoteDevice(mac);
        Bus.sendState(this, "Connecting…");
        smooth.clear();
        lastContactDetected = false;
        gatt = dev.connectGatt(this, false, cb, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback cb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                reconnecting = false;
                Bus.sendState(BleService.this, "Discovering…");
                g.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Bus.sendState(BleService.this, "Disconnected");
                lastContactDetected = false;
                if (prefs.getBoolean("auto_reconnect", true) && !reconnecting) {
                    reconnecting = true;
                    g.close();
                    String last = prefs.getString(PREF_LAST, null);
                    getMainLooper().getQueue().addIdleHandler(() -> {
                        connect(last);
                        return false;
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            BluetoothGattService svc = g.getService(UUID_SERVICE_HR);
            if (svc == null) {
                Bus.sendError(BleService.this, "HR service not found");
                return;
            }
            BluetoothGattCharacteristic ch = svc.getCharacteristic(UUID_CHAR_HR);
            if (ch == null) {
                Bus.sendError(BleService.this, "HR characteristic not found");
                return;
            }

            g.setCharacteristicNotification(ch, true);
            BluetoothGattDescriptor cccd = ch.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            );
            if (cccd != null) {
                cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(cccd);
            }
            Bus.sendState(BleService.this, "Connected");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            if (UUID_CHAR_HR.equals(c.getUuid())) {
                int raw = parseHr(c.getValue());

                // DS-04.1: Ignore malformed / invalid frames (parseHr returns -1).
                if (raw <= 0) {
                    return; // Don't update UI with garbage or crash.
                }

                int bpm = smooth(raw);
                Bus.sendHr(BleService.this, bpm);
            }
        }
    };

    /**
     * Parse a Bluetooth SIG Heart Rate Measurement value.
     *
     * Format (Bluetooth SIG spec):
     *  - Byte 0: Flags
     *      bit 0: 0 = uint8 HR, 1 = uint16 HR
     *      bit 1: Sensor contact supported
     *      bit 2: Sensor contact detected (if bit 1 is set)
     *      bit 3: Energy expended present (2 bytes)
     *      bit 4: RR-interval present (2*n bytes)
     *
     *  - Next: Heart Rate Measurement Value (8 or 16 bit)
     *  - Optional fields follow (not fully parsed here, but length is validated).
     *
     *  Returns:
     *      - BPM >= 1 if valid
     *      - -1 if malformed or unusable
     */
    private int parseHr(byte[] v) {
        if (v == null || v.length < 2) {
            return -1; // Malformed: need at least flags + 1 byte HR.
        }

        int offset = 0;

        // Flags byte
        int flags = v[offset++] & 0xFF;
        boolean is16Bit = (flags & 0x01) != 0;
        boolean contactSupported = (flags & 0x02) != 0;
        boolean contactDetected = (flags & 0x04) != 0;
        boolean energyPresent = (flags & 0x08) != 0;
        boolean rrPresent = (flags & 0x10) != 0;

        // DS-04.1: track sensor-contact bits (for future UI / logic if needed)
        lastContactDetected = contactSupported && contactDetected;

        int bpm;

        // Heart Rate value (8-bit or 16-bit)
        if (is16Bit) {
            if (v.length < offset + 2) {
                return -1; // Malformed frame: says 16-bit but not enough bytes.
            }
            // Little-endian 16-bit
            bpm = ((v[offset] & 0xFF) | ((v[offset + 1] & 0xFF) << 8));
            offset += 2;
        } else {
            if (v.length < offset + 1) {
                return -1; // Malformed frame: says 8-bit but no data.
            }
            bpm = v[offset] & 0xFF;
            offset += 1;
        }

        // Optionally skip energy expended (2 bytes) if present.
        if (energyPresent) {
            if (v.length < offset + 2) {
                return -1; // Malformed: flags claim energy but not enough bytes.
            }
            offset += 2;
        }

        // Optionally validate RR-interval bytes count (2 bytes per interval).
        if (rrPresent) {
            int remaining = v.length - offset;
            if (remaining < 2 || (remaining % 2) != 0) {
                // RR present but length not multiple of 2 => malformed.
                return -1;
            }
            // We don't actually need RR intervals for now, so we just accept them.
        }

        // Basic sanity check: BPM must be positive to be considered valid.
        if (bpm <= 0) {
            return -1;
        }

        return bpm;
    }

    private int smooth(int value) {
        if (value <= 0) return value;
        if (smooth.size() == 5) smooth.poll();
        smooth.offer(value);
        int sum = 0;
        for (int x : smooth) sum += x;
        return sum / smooth.size();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (gatt != null) {
            gatt.close();
            gatt = null;
        }
    }
}