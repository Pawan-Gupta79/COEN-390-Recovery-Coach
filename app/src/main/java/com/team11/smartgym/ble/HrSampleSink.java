package com.team11.smartgym.ble;

import android.os.Handler;
import android.os.Looper;

import com.team11.smartgym.ui.session.SessionViewModel;

/**
 * Thread-safe bridge between BLE notifications and SessionViewModel.
 *
 * BLE callbacks happen on binder threads, not the UI thread — so this class
 * posts BPM values to the main thread safely.
 */
public final class HrSampleSink {

    private final SessionViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HrSampleSink(SessionViewModel vm) {
        this.viewModel = vm;
    }

    /** Call this from your BLE HR notification callback. */
    public void onHrValue(final int bpm) {
        // Ensure we always update ViewModel on main/UI thread
        mainHandler.post(() -> viewModel.onHeartRate(bpm));
    }
}
