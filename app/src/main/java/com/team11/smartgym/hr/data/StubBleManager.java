package com.team11.smartgym.hr.data;

import android.os.Handler;
import android.os.Looper;

public class StubBleManager implements BleManager {

    private boolean connected = false;
    private OnConnectionChangedListener connectionListener;
    private OnHeartRateListener heartRateListener;
    private OnErrorListener errorListener;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // UI-04.2 Test flag: auto-disconnect once after first connect
    private boolean testAutoDropOnce = true;

    @Override
    public void connect() {
        // Simulate async connect after 700ms
        handler.postDelayed(() -> {
            connected = true;
            if (connectionListener != null) connectionListener.onConnected();

            // --- TEST HOOK: simulate unexpected disconnect once ---
            if (testAutoDropOnce) {
                testAutoDropOnce = false;
                handler.postDelayed(this::disconnect, 2000); // drop after 2s
            }
            // -------------------------------------------------------
        }, 700);
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        if (connectionListener != null) connectionListener.onDisconnected();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void startHeartRate() {
        if (!connected) return;

        // Emit fake HR every 1s
        handler.postDelayed(new Runnable() {
            int hr = 78;

            @Override
            public void run() {
                if (!connected) return;

                // small random HR variation
                hr += (int) (Math.random() * 6 - 3);
                if (hr < 50) hr = 50;
                if (hr > 170) hr = 170;

                if (heartRateListener != null) heartRateListener.onHeartRate(hr);

                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public void stopHeartRate() {
        // No-op for stub
    }

    @Override
    public void setConnectionListener(OnConnectionChangedListener listener) {
        connectionListener = listener;
    }

    @Override
    public void setHeartRateListener(OnHeartRateListener listener) {
        heartRateListener = listener;
    }

    @Override
    public void setErrorListener(OnErrorListener listener) {
        errorListener = listener;
    }
}
