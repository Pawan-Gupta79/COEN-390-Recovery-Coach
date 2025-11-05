package com.team11.smartgym.hr.ui.livehr;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.hr.data.BleManager;
import com.team11.smartgym.hr.data.StubBleManager; // swap to real manager later
import com.team11.smartgym.hr.domain.ConnectionState;
import com.team11.smartgym.hr.util.ErrorMapper;
import com.team11.smartgym.hr.util.Event;
import com.team11.smartgym.hr.util.UiError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ViewModel that:
 * - Listens for heart-rate values and updates BPM + lastUpdated
 * - Emits UiError events when BLE reports errors
 * - Keeps a ConnectionState for the UI to react to
 */
public class HeartRateViewModel extends ViewModel {

    private final BleManager ble;

    private final MutableLiveData<Integer> bpm = new MutableLiveData<>(null);
    private final MutableLiveData<String> lastUpdated = new MutableLiveData<>("");
    private final MutableLiveData<ConnectionState> connection =
            new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final Event<UiError> uiError = new Event<>();

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    /** Default constructor uses the stub to keep things compiling/runnable. */
    public HeartRateViewModel() {
        this(new StubBleManager());
    }

    /** If you later inject a real BleManager, use this constructor. */
    public HeartRateViewModel(BleManager manager) {
        this.ble = (manager != null) ? manager : new StubBleManager();
        attachCallbacks();
    }

    private void attachCallbacks() {
        ble.setConnectionListener(new BleManager.OnConnectionChangedListener() {
            @Override public void onConnected() { connection.postValue(ConnectionState.CONNECTED); }
            @Override public void onDisconnected() { connection.postValue(ConnectionState.DISCONNECTED); }
        });

        ble.setHeartRateListener(value -> {
            bpm.postValue(value);
            lastUpdated.postValue(timeFmt.format(new Date()));
            connection.postValue(ConnectionState.CONNECTED);
        });

        ble.setErrorListener(error -> {
            connection.postValue(ConnectionState.ERROR);
            uiError.emit(ErrorMapper.from(error));
        });
    }

    public LiveData<Integer> bpm() { return bpm; }
    public LiveData<String> lastUpdated() { return lastUpdated; }
    public LiveData<ConnectionState> connection() { return connection; }
    public Event<UiError> uiError() { return uiError; }

    /** Call from Fragment.onResume() */
    public void start() {
        connection.setValue(ConnectionState.CONNECTING);
        ble.connect();
        ble.startHeartRate();
    }

    /** Call from Fragment.onPause() */
    public void stop() {
        ble.stopHeartRate();
        ble.disconnect();
    }
}
