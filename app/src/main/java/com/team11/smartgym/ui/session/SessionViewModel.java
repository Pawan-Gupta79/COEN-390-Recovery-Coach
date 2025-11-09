package com.team11.smartgym.ui.session;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.team11.smartgym.data.SessionController;
import com.team11.smartgym.data.TempSessionSnapshot;

public class SessionViewModel extends AndroidViewModel {

    private final SessionController controller;

    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private final MutableLiveData<TempSessionSnapshot> lastSnapshot = new MutableLiveData<>(null);

    public SessionViewModel(@NonNull Application application) {
        super(application);
        controller = new SessionController(application.getApplicationContext());
        // Preload any previously saved snapshot (optional)
        lastSnapshot.setValue(controller.loadLastSnapshot());
    }

    public LiveData<Boolean> isRunning() { return running; }
    public LiveData<TempSessionSnapshot> lastSnapshot() { return lastSnapshot; }

    /** Start a new live session (resets buffers). */
    public void startSession() {
        controller.start();
        running.setValue(true);
    }

    /** Stop, compute stats, save snapshot (carry-over), reset buffers. */
    public void stopAndSave() {
        TempSessionSnapshot snap = controller.stopAndSave();
        lastSnapshot.setValue(snap);
        running.setValue(false);
    }

    /** Feed heart rate samples during the live session (hook BLE callback to this). */
    public void onHeartRate(int bpm) {
        controller.addSample(bpm);
    }

    /** Optional helpers */
    public void clearLastSnapshot() {
        controller.clearLastSnapshot();
        lastSnapshot.setValue(null);
    }
}
