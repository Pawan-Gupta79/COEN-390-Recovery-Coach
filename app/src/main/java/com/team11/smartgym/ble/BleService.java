package com.team11.smartgym.ble;

import android.app.*;
import android.bluetooth.*;
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
    // Replace with your custom UUIDs if your ESP32 exposes different ones.
    private static final UUID UUID_SERVICE_HR = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB");
    private static final UUID UUID_CHAR_HR   = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private SharedPreferences prefs;
    private boolean reconnecting = false;

    private final Queue<Integer> smooth = new ArrayDeque<>();

    @Override public void onCreate() {
        super.onCreate();
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bm.getAdapter();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        startForegroundWithNotif("Idle");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CONNECT.equals(intent.getAction())) {
            String mac = intent.getStringExtra(EXTRA_DEVICE);
            prefs.edit().putString(PREF_LAST, mac).apply();
            connect(mac);
        } else if (intent != null && ACTION_START.equals(intent.getAction())) {
            String last = prefs.getString(PREF_LAST, null);
            if (last != null && prefs.getBoolean("auto_reconnect", true)) connect(last);
        }
        return START_STICKY;
    }

    private void startForegroundWithNotif(String state) {
        String chId = "ble";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(chId, "BLE", NotificationManager.IMPORTANCE_LOW);
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
        if (mac == null) { Bus.sendError(this, "No device selected"); return; }
        BluetoothDevice dev = adapter.getRemoteDevice(mac);
        Bus.sendState(this, "Connecting…");
        smooth.clear();
        gatt = dev.connectGatt(this, false, cb, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback cb = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                reconnecting = false;
                Bus.sendState(BleService.this, "Discovering…");
                g.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Bus.sendState(BleService.this, "Disconnected");
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

        @Override public void onServicesDiscovered(BluetoothGatt g, int status) {
            BluetoothGattService svc = g.getService(UUID_SERVICE_HR);
            if (svc == null) { Bus.sendError(BleService.this, "HR service not found"); return; }
            BluetoothGattCharacteristic ch = svc.getCharacteristic(UUID_CHAR_HR);
            if (ch == null) { Bus.sendError(BleService.this, "HR characteristic not found"); return; }

            g.setCharacteristicNotification(ch, true);
            BluetoothGattDescriptor cccd = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (cccd != null) {
                cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(cccd);
            }
            Bus.sendState(BleService.this, "Connected");
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            if (UUID_CHAR_HR.equals(c.getUuid())) {
                int raw = parseHr(c.getValue());
                int bpm = smooth(raw);
                Bus.sendHr(BleService.this, bpm);
            }
        }
    };

    private int parseHr(byte[] v) {
        if (v == null || v.length == 0) return -1;
        int flags = v[0] & 0xFF;
        boolean is16 = (flags & 0x01) != 0;
        if (is16 && v.length >= 3) {
            ByteBuffer bb = ByteBuffer.wrap(v, 1, 2).order(ByteOrder.LITTLE_ENDIAN);
            return bb.getShort() & 0xFFFF;
        } else if (v.length >= 2) {
            return v[1] & 0xFF;
        }
        return -1;
    }

    private int smooth(int value) {
        if (value <= 0) return value;
        if (smooth.size() == 5) smooth.poll();
        smooth.offer(value);
        int sum = 0;
        for (int x : smooth) sum += x;
        return sum / smooth.size();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        super.onDestroy();
        if (gatt != null) { gatt.close(); gatt = null; }
    }
}