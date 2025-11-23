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
import android.os.Handler;
import android.os.Looper;
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
    public static final String ACTION_CANCEL_RECONNECT = "CANCEL_RECONNECT";
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

    // DS-05: Reconnect state machine.
    private enum ReconnectState {
        IDLE,
        RECONNECTING,
        CONNECTED,
        FAILED
    }

    private ReconnectState reconnectState = ReconnectState.IDLE;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long BASE_RECONNECT_DELAY_MS = 2_000L;
    private static final long MAX_RECONNECT_DELAY_MS = 30_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingReconnect;

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
        } else if (intent != null && ACTION_CANCEL_RECONNECT.equals(intent.getAction())) {
            // Allow UI / callers to explicitly cancel any reconnect attempts when
            // the user navigates away from the BLE-dependent screens.
            cancelReconnect();
        }
        return START_STICKY;
    }

    private void startForegroundWithNotif(String state) {
        String chId = "ble";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    chId, "BLE",
                    NotificationManager.IMPORTANCE_MIN
            );
            nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, chId)
                .setContentTitle("SmartGym BLE")
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

        // Any explicit connect (either initial or from the reconnect timer)
        // cancels pending reconnect callbacks so we don't double-connect.
        cancelPendingReconnectInternal();

        if (gatt != null) {
            gatt.close();
            gatt = null;
        }

        BluetoothDevice dev = adapter.getRemoteDevice(mac);
        Bus.sendState(this, "Connecting…");
        smooth.clear();
        lastContactDetected = false;
        gatt = dev.connectGatt(this, false, cb, BluetoothDevice.TRANSPORT_LE);
    }

    // -----------------------------
    // DS-05: Reconnect state machine
    // -----------------------------

    /**
     * Compute exponential backoff delay in milliseconds, clamped to a maximum.
     */
    private long computeBackoffDelayMs(int attempt) {
        if (attempt <= 1) {
            return BASE_RECONNECT_DELAY_MS;
        }
        long delay = (long) (BASE_RECONNECT_DELAY_MS * Math.pow(2, attempt - 1));
        return Math.min(delay, MAX_RECONNECT_DELAY_MS);
    }

    /**
     * Schedule a reconnect attempt using the last known device address.
     * This method moves the internal state machine into RECONNECTING.
     */
    private void scheduleReconnect() {
        String last = prefs.getString(PREF_LAST, null);
        if (last == null) {
            // No known device to reconnect to.
            reconnectState = ReconnectState.FAILED;
            Bus.sendState(this, "ReconnectFailedNoDevice");
            return;
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            // We've already tried too many times in this session.
            reconnectState = ReconnectState.FAILED;
            Bus.sendState(this, "ReconnectFailed");
            return;
        }

        reconnectState = ReconnectState.RECONNECTING;
        reconnectAttempts++;

        long delay = computeBackoffDelayMs(reconnectAttempts);

        // Notify UI that we're going to try again after a delay.
        Bus.sendState(this, "Reconnecting… (" + reconnectAttempts + ")");

        cancelPendingReconnectInternal();

        pendingReconnect = () -> {
            // If the user navigated away / canceled, don't reconnect.
            if (reconnectState != ReconnectState.RECONNECTING) {
                return;
            }
            connect(last);
        };

        mainHandler.postDelayed(pendingReconnect, delay);
    }

    /**
     * Internal helper to clear the pending reconnect Runnable, if any.
     */
    private void cancelPendingReconnectInternal() {
        if (pendingReconnect != null) {
            mainHandler.removeCallbacks(pendingReconnect);
            pendingReconnect = null;
        }
    }

    /**
     * Public-facing cancel entry point for UI / callers.
     * Resets the reconnect state machine back to IDLE.
     */
    private void cancelReconnect() {
        reconnectState = ReconnectState.IDLE;
        reconnectAttempts = 0;
        cancelPendingReconnectInternal();
    }

    private final BluetoothGattCallback cb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Successful connection: reset the reconnect state machine.
                cancelPendingReconnectInternal();
                reconnectState = ReconnectState.CONNECTED;
                reconnectAttempts = 0;

                Bus.sendState(BleService.this, "Discovering…");
                g.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Bus.sendState(BleService.this, "Disconnected");
                lastContactDetected = false;

                // Always close the current GATT on disconnect.
                if (gatt != null) {
                    gatt.close();
                    gatt = null;
                } else {
                    g.close();
                }

                // If auto-reconnect is enabled, drive the reconnect state machine.
                if (prefs.getBoolean("auto_reconnect", true)) {
                    scheduleReconnect();
                } else {
                    reconnectState = ReconnectState.IDLE;
                    reconnectAttempts = 0;
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
            BluetoothGattDescriptor ccc = ch.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"));
            if (ccc != null) {
                ccc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(ccc);
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
     * Parse Heart Rate Measurement characteristic according to Bluetooth SIG spec.
     * Returns -1 for malformed or unsupported frames.
     */
    private int parseHr(byte[] v) {
        if (v == null || v.length == 0) {
            return -1;
        }

        int offset = 0;
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
                return -1; // Malformed frame: says 16-bit
            }
            bpm = ((v[offset] & 0xFF) | ((v[offset + 1] & 0xFF) << 8));
            offset += 2;
        } else {
            if (v.length < offset + 1) {
                return -1; // Malformed frame: says 8-bit
            }
            bpm = v[offset] & 0xFF;
            offset += 1;
        }

        // Optional Energy Expended field
        if (energyPresent) {
            if (v.length < offset + 2) {
                return -1;
            }
            // We read but ignore the value for now.
            int energy = ((v[offset] & 0xFF) | ((v[offset + 1] & 0xFF) << 8));
            offset += 2;
        }

        // Optional RR-Interval field(s)
        if (rrPresent) {
            // Each RR interval is a 16-bit value in 1/1024 second units.
            // We don't need the actual value for now, but we validate the length.
            int remaining = v.length - offset;
            if ((remaining % 2) != 0) {
                return -1; // Malformed RR intervals
            }

            // Example of parsing first RR interval if ever needed:
            if (remaining >= 2) {
                int rr = ((v[offset] & 0xFF) | ((v[offset + 1] & 0xFF) << 8));
                // rrMs = (rr / 1024.0f) * 1000.0f;
            }
        }

        return bpm;
    }

    /**
     * Simple moving-average smoother for heart rate samples.
     */
    private int smooth(int rawBpm) {
        // Maintain a small window of the last N samples.
        final int WINDOW = 5;
        smooth.add(rawBpm);
        while (smooth.size() > WINDOW) {
            smooth.poll();
        }

        int sum = 0;
        for (int v : smooth) {
            sum += v;
        }
        return sum / smooth.size();
    }

    /**
     * Expose last known sensor-contact status (for future use).
     */
    public boolean isLastContactDetected() {
        return lastContactDetected;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Tear down any pending reconnect callbacks when the service is destroyed.
        cancelReconnect();
        if (gatt != null) {
            gatt.close();
            gatt = null;
        }
    }
}
