package com.team11.smartgym.hr.data;

import android.os.Handler;
import android.os.Looper;

import com.team11.smartgym.BuildConfig;

import java.util.Random;

public class StubBleManager implements BleManager {

    private boolean connected = false;
    private OnConnectionChangedListener connectionListener;
    private OnHeartRateListener heartRateListener;
    private OnErrorListener errorListener;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();

    private long suppressHrUntilMs = 0L;

    // Deterministic timeout pacing (DEBUG only)
    private int tickCount = 0;
    private boolean firstTimeoutSent = false;
    private int ticksBetweenTimeouts = 20; // ~20s between timeouts
    private int firstTimeoutAtTick = 8;    // first at ~8s
    private int nextTimeoutTick = -1;

    @Override
    public void connect() {
        handler.postDelayed(() -> {
            connected = true;
            if (connectionListener != null) connectionListener.onConnected();
        }, 700);
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        if (connectionListener != null) connectionListener.onDisconnected();
    }

    @Override
    public boolean isConnected() { return connected; }

    @Override
    public void startHeartRate() {
        if (!connected) return;

        handler.postDelayed(new Runnable() {
            int hr = 78;

            @Override
            public void run() {
                if (!connected) return;

                tickCount++;

                if (BuildConfig.DEBUG) {
                    if (!firstTimeoutSent && tickCount >= firstTimeoutAtTick) {
                        fireTimeoutAndPause();
                        firstTimeoutSent = true;
                        nextTimeoutTick = tickCount + ticksBetweenTimeouts;
                    } else if (firstTimeoutSent && nextTimeoutTick > 0 && tickCount >= nextTimeoutTick) {
                        fireTimeoutAndPause();
                        nextTimeoutTick = tickCount + ticksBetweenTimeouts;
                    }
                } else {
                    if (errorListener != null && rnd.nextInt(50) == 0) {
                        fireTimeoutAndPause();
                    }
                }

                // Emit HR only when not suppressed (simulate a stall)
                if (System.currentTimeMillis() >= suppressHrUntilMs) {
                    hr += rnd.nextInt(7) - 3;
                    if (hr < 50) hr = 50;
                    if (hr > 170) hr = 170;
                    if (heartRateListener != null) heartRateListener.onHeartRate(hr);
                }

                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void fireTimeoutAndPause() {
        if (errorListener != null) {
            errorListener.onError(new BleError(BleError.Code.GATT_TIMEOUT));
        }
        // Pause HR emissions for ~7s to give the user time to see & tap Retry
        suppressHrUntilMs = System.currentTimeMillis() + 7000L;
    }

    @Override public void stopHeartRate() { /* loop ends when disconnected */ }
    @Override public void setConnectionListener(OnConnectionChangedListener l) { connectionListener = l; }
    @Override public void setHeartRateListener(OnHeartRateListener l) { heartRateListener = l; }
    @Override public void setErrorListener(OnErrorListener l) { errorListener = l; }
}
