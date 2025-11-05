package com.team11.smartgym.hr.ui.livehr;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.hr.data.BleManager;
import com.team11.smartgym.hr.domain.ConnectionState;
import com.team11.smartgym.hr.util.ErrorMapper;
import com.team11.smartgym.hr.util.Event;
import com.team11.smartgym.hr.util.UiError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HeartRateViewModel extends ViewModel {

    private final BleManager ble;

    private final MutableLiveData<Integer> bpm = new MutableLiveData<>(null);
    private final MutableLiveData<String> lastUpdated = new MutableLiveData<>("");
    private final MutableLiveData<ConnectionState> connection =
            new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final Event<UiError> uiError = new Event<>();

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());


    public HeartRateViewModel(BleManager manager) {
        this.ble = (manager != null) ? manager : new StubBleManager();
        attachCallbacks();
    }

    private void attachCallbacks() {
        ble.setConnectionListener(new BleManager.OnConnectionChangedListener() {
        });

        ble.setHeartRateListener(value -> {
            bpm.postValue(value);
            lastUpdated.postValue(timeFmt.format(new Date()));
            connection.postValue(ConnectionState.CONNECTED);
        });

    }

    public LiveData<Integer> bpm() { return bpm; }
    public LiveData<String> lastUpdated() { return lastUpdated; }
    public LiveData<ConnectionState> connection() { return connection; }
    public Event<UiError> uiError() { return uiError; }

    public void start() {
        connection.setValue(ConnectionState.CONNECTING);
        ble.connect();
        ble.startHeartRate();
    }

    public void stop() {
        ble.stopHeartRate();
        ble.disconnect();
    }
}
