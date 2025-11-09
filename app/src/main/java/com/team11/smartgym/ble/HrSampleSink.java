package com.team11.smartgym.ble;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.team11.smartgym.data.SessionController;

/**
 * HrSampleSink: receives raw BPM from BLE and forwards to SessionController.
 * NO ViewModel dependency (hardware → controller only).
 */
public final class HrSampleSink {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SessionController controller;

    public HrSampleSink(@NonNull SessionController controller) {
        this.controller = controller;
    }

    /** Common entry (if your BLE stack calls this) */
    public void onHeartRate(final int bpm) {
        forward(bpm);
    }

    /** Alternate name kept for compatibility with existing call sites */
    public void onNewHeartRate(final int bpm) {
        forward(bpm);
    }

    private void forward(final int bpm) {
        mainHandler.post(() -> {
            if (controller != null) {
                controller.addHeartRate(System.currentTimeMillis(), bpm);
            }
        });
    }
}
