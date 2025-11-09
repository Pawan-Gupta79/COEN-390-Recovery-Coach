package com.team11.smartgym.ui.session;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.SessionController;
import com.team11.smartgym.data.TempSessionSnapshot;

/**
 * ViewModel for session controls screen.
 * - start/stop session
 * - accept live HR samples
 * - expose running state
 * - expose last saved snapshot
 * - expose live sample count (for quick feedback while running)
 */
public class SessionViewModel extends AndroidViewModel {

    private final SessionController controller;

    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private final MutableLiveData<TempSessionSnapshot> lastSnapshot = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> liveSampleCount = new MutableLiveData<>(0);

    public SessionViewModel(@NonNull Application app) {
        super(app);
        controller = DatabaseProvider.get(app).getSessionController();
    }

    // ---- Exposed state ----
    public LiveData<Boolean> isRunning() { return running; }
    public LiveData<TempSessionSnapshot> lastSnapshot() { return lastSnapshot; }
    public LiveData<Integer> liveSampleCount() { return liveSampleCount; }

    // ---- Actions ----
    public void start() {
        if (Boolean.TRUE.equals(running.getValue())) return;
        controller.startSession();
        running.setValue(true);
        liveSampleCount.setValue(0);
    }

    public void stop() {
        if (!Boolean.TRUE.equals(running.getValue())) return;
        TempSessionSnapshot snap = controller.stopSessionAndReturnSnapshot();
        lastSnapshot.setValue(snap);          // show snapshot after STOP
        running.setValue(false);
    }

    /** Accept a BPM reading (used by Fake button or BLE sink). */
    public void onHeartRate(int bpm) {
        if (!Boolean.TRUE.equals(running.getValue())) return;
        controller.onHeartRate(bpm);          // forwards to repo as Reading
        Integer n = liveSampleCount.getValue();
        liveSampleCount.setValue((n == null ? 0 : n) + 1);  // live feedback
    }
}
