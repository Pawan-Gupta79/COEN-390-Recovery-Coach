package com.team11.smartgym.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.model.ConnectionState;
import com.team11.smartgym.ui.common.FakeHrGenerator;

public class ConnectionViewModel extends ViewModel {

    private final MutableLiveData<ConnectionState> state =
            new MutableLiveData<>(ConnectionState.DISCONNECTED);

    private final MutableLiveData<String> deviceName = new MutableLiveData<>("");
    private final MutableLiveData<String> deviceAddr = new MutableLiveData<>("");

    // Live BPM for UI-02.2/02.3
    private final MutableLiveData<Integer> bpm = new MutableLiveData<>(null);

    private boolean autoReconnect = false;

    private FakeHrGenerator fake;

    // ----- State -----
    public LiveData<ConnectionState> getState() { return state; }
    public void setState(ConnectionState s) { state.setValue(s); }

    public LiveData<String> getDeviceName() { return deviceName; }
    public void setDevice(String name, String addr) {
        deviceName.setValue(name);
        deviceAddr.setValue(addr);
    }

    public boolean isAutoReconnectEnabled() { return autoReconnect; }
    public void setAutoReconnectEnabled(boolean enabled) { autoReconnect = enabled; }

    // ----- BPM LiveData -----
    public LiveData<Integer> getBpm() { return bpm; }
    private void setBpm(Integer value) { bpm.setValue(value); }

    // ----- Fake Sensor control -----
    public void startFakeSensor() {
        if (fake != null) return; // already running
        fake = new FakeHrGenerator(value -> setBpm(value));
        fake.start();
    }

    public void stopFakeSensor() {
        if (fake != null) {
            fake.stop();
            fake = null;
        }
        setBpm(null); // show "--"
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopFakeSensor();
    }
}
