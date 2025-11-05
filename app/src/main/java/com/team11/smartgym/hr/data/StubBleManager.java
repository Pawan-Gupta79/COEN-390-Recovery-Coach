package com.team11.smartgym.hr.data;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;

/**
 * TEMP implementation to keep the app compiling and to drive the UI/ViewModel.
 * Simulates connection, heart-rate ticks, rare transient errors, and disconnects.
 * Replace with the real BLE implementation later.
 */
public class StubBleManager implements BleManager {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private OnHeartRateListener hrListener;
    private OnErrorListener errorListener;
    private OnConnectionChangedListener connectionListener;

    private boolean connected = false;
    private boolean streaming = false;

    private final Runnable hrTicker = new Runnable() {
        @Override public void run() {
            if (connected && streaming && hrListener != null) {
                int bpm = 60 + random.nextInt(80); // 60..139
                hrListener.onHeartRate(bpm);

                // Simulate a rare transient error
                if (random.nextInt(120) == 0 && errorListener != null) {
                    errorListener.onError(new RuntimeException("Simulated GATT timeout"));
                }
            }
            if (connected && streaming) {
                handler.postDelayed(this, 1000); // 1 Hz updates
            }
        }
    };

    @Override public void connect() {
        // Simulate async connection after 700ms
        handler.postDelayed(() -> {
            connected = true;
            if (connectionListener != null) connectionListener.onConnected();
        }, 700);
    }

    @Override public void disconnect() {
        boolean wasConnected = connected;
        connected = false;
        streaming = false;
        handler.removeCallbacks(hrTicker);
        if (wasConnected && connectionListener != null) connectionListener.onDisconnected();
    }

    @Override public void startHeartRate() {
        if (!connected || streaming) return;
        streaming = true;
        handler.post(hrTicker);
    }

    @Override public void stopHeartRate() {
        streaming = false;
        handler.removeCallbacks(hrTicker);
    }

    @Override public boolean isConnected() {
        return connected;
    }

    @Override public void setHeartRateListener(OnHeartRateListener l) { this.hrListener = l; }
    @Override public void setErrorListener(OnErrorListener l) { this.errorListener = l; }
    @Override public void setConnectionListener(OnConnectionChangedListener l) { this.connectionListener = l; }
}
