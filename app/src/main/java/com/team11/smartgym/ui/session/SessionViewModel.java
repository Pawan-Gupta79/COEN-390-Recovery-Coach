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
 * ViewModel that exposes session controls and last snapshot to the UI.
 * Now extends AndroidViewModel so we can access application context.
 */
public class SessionViewModel extends AndroidViewModel {

    private final SessionController controller;

    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private final MutableLiveData<TempSessionSnapshot> lastSnapshot = new MutableLiveData<>(null);

    public SessionViewModel(@NonNull Application app) {
        super(app);
        controller = DatabaseProvider.get(app).getSessionController();
    }

    // LiveData getters
    public LiveData<Boolean> isRunning() { return running; }
    public LiveData<TempSessionSnapshot> lastSnapshot() { return lastSnapshot; }

    // Actions
    public void start() {
        controller.startSession();
        running.postValue(true);
    }

    public void stop() {
        TempSessionSnapshot snap = controller.stopSessionAndReturnSnapshot();
        running.postValue(false);
        lastSnapshot.postValue(snap);
    }

    public void onHeartRate(int bpm) {
        controller.onHeartRate(bpm);
    }
}
