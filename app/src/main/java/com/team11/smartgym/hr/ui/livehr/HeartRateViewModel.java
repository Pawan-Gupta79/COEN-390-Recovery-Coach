package com.team11.smartgym.hr.ui.livehr;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.hr.data.BleManager;
import com.team11.smartgym.hr.data.StubBleManager;
import com.team11.smartgym.hr.domain.ConnectionState;
import com.team11.smartgym.hr.util.ErrorMapper;
import com.team11.smartgym.hr.util.Event;
import com.team11.smartgym.hr.util.UiError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HeartRateViewModel extends ViewModel {

    private final BleManager ble;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Integer> bpm = new MutableLiveData<>(null);
    private final MutableLiveData<String> lastUpdated = new MutableLiveData<>("");
    private final MutableLiveData<ConnectionState> connection =
            new MutableLiveData<>(ConnectionState.DISCONNECTED);

    private final Event<UiError> uiError = new Event<>();
    private final Event<String> uiSnackbar = new Event<>();

    private final MutableLiveData<Boolean> retrying = new MutableLiveData<>(false);

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private boolean userInitiatedStop = false;
    private boolean hasRetriedThisDisconnect = false;
    private boolean inForeground = true;
    private boolean isStreaming = false;

    private Runnable pendingRetryTimeoutCheck = null;

    public HeartRateViewModel() { this(new StubBleManager()); }
    public HeartRateViewModel(BleManager manager) {
        this.ble = (manager != null) ? manager : new StubBleManager();
        attachCallbacks();
    }

    private void attachCallbacks() {
        ble.setConnectionListener(new BleManager.OnConnectionChangedListener() {
            @Override public void onConnected() {
                connection.postValue(ConnectionState.CONNECTED);
                retrying.postValue(false);
                uiSnackbar.emit("Reconnected");
                if (isStreaming) ble.startHeartRate();
            }

            @Override public void onDisconnected() {
                connection.postValue(ConnectionState.DISCONNECTED);
                // Auto-retry once on unexpected drop
                if (inForeground && !userInitiatedStop && !hasRetriedThisDisconnect) {
                    scheduleSingleAutoRetry(4000L);
                }
            }
        });

        ble.setHeartRateListener(value -> {
            bpm.postValue(value);
            lastUpdated.postValue(timeFmt.format(new Date()));
            connection.postValue(ConnectionState.CONNECTED);
        });

        ble.setErrorListener(error -> uiError.emit(ErrorMapper.from(error)));
    }

    public LiveData<Integer> bpm() { return bpm; }
    public LiveData<String> lastUpdated() { return lastUpdated; }
    public LiveData<ConnectionState> connection() { return connection; }
    public Event<UiError> uiError() { return uiError; }
    public Event<String> uiSnackbar() { return uiSnackbar; }
    public LiveData<Boolean> retrying() { return retrying; }

    public void start() {
        if (isStreaming && (connection.getValue() == ConnectionState.CONNECTED
                || connection.getValue() == ConnectionState.CONNECTING)) return;

        userInitiatedStop = false;
        hasRetriedThisDisconnect = false;
        isStreaming = true;

        connection.setValue(ConnectionState.CONNECTING);
        ble.connect();
        ble.startHeartRate();
    }

    public void stop() {
        userInitiatedStop = true;
        isStreaming = false;
        cancelPendingRetry();
        retrying.setValue(false);

        ble.stopHeartRate();
        ble.disconnect();

        connection.setValue(ConnectionState.DISCONNECTED);
        bpm.setValue(null);
        lastUpdated.setValue("");
    }

    public void setForeground(boolean isForeground) {
        inForeground = isForeground;
        if (!inForeground) {
            cancelPendingRetry();
            retrying.setValue(false);
        }
    }

    /** Manual Retry invoked by UI: force a real reconnect cycle. */
    public void retryNow() {
        retrying.setValue(true);
        uiSnackbar.emit("Reconnecting…");

        // Clean reconnect: disconnect -> short delay -> connect + resume stream
        ble.stopHeartRate();
        ble.disconnect();
        connection.postValue(ConnectionState.CONNECTING);

        handler.postDelayed(() -> {
            ble.connect();
            if (isStreaming) ble.startHeartRate();
        }, 350);
    }

    /** Auto-retry once after unexpected disconnect. */
    private void scheduleSingleAutoRetry(long timeoutMillis) {
        hasRetriedThisDisconnect = true;
        retrying.postValue(true);
        uiSnackbar.emit("Reconnecting…");
        connection.postValue(ConnectionState.CONNECTING);
        ble.connect();

        cancelPendingRetry();
        pendingRetryTimeoutCheck = () -> {
            pendingRetryTimeoutCheck = null;

            if (!inForeground) { retrying.postValue(false); return; }

            if (ble.isConnected()) {
                connection.postValue(ConnectionState.CONNECTED);
                uiSnackbar.emit("Reconnected");
                retrying.postValue(false);
                if (isStreaming) ble.startHeartRate();
            } else {
                connection.postValue(ConnectionState.ERROR);
                retrying.postValue(false);
                uiError.emit(new UiError("Reconnect failed", "Retry", false));
            }
        };
        handler.postDelayed(pendingRetryTimeoutCheck, timeoutMillis);
    }

    private void cancelPendingRetry() {
        if (pendingRetryTimeoutCheck != null) {
            handler.removeCallbacks(pendingRetryTimeoutCheck);
            pendingRetryTimeoutCheck = null;
        }
    }
}
