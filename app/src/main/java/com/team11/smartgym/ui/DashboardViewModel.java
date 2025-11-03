package com.team11.smartgym.ui;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.model.ConnectionState;

import java.util.Random;

/**
 * Single ViewModel for Dashboard:
 * - Connection state (Disconnected/Connecting/Connected/Reconnecting)
 * - Device name/address
 * - BPM stream (nullable). Fake-sensor support with 1s updates
 * - Auto-reconnect + Fake-sensor flags
 */
public class DashboardViewModel extends ViewModel {

    // ----- Connection / device -----
    private final MutableLiveData<ConnectionState> state = new MutableLiveData<>();
    private final MutableLiveData<String> deviceName = new MutableLiveData<>("");
    private @Nullable String deviceAddr;

    // ----- BPM (nullable) -----
    private final MutableLiveData<Integer> bpm = new MutableLiveData<>(null);

    // ----- Flags -----
    private boolean autoReconnectEnabled = false;
    private boolean fakeSensorEnabled = false;

    // ----- Fake sensor generator -----
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();
    private int fakeCurrent = 72; // start near resting HR
    private final Runnable fakeTick = new Runnable() {
        @Override public void run() {
            if (fakeSensorEnabled) {
                // small random walk within a reasonable band
                int delta = rng.nextInt(5) - 2; // -2..+2
                fakeCurrent += delta;
                if (fakeCurrent < 55) fakeCurrent = 55;
                if (fakeCurrent > 140) fakeCurrent = 140;

                bpm.setValue(fakeCurrent);
                // throttle UI to ~1s
                handler.postDelayed(this, 1000);
            }
        }
    };

    // ====== Public API used by DashboardFragment ======

    // --- Auto-reconnect flag
    public void setAutoReconnectEnabled(boolean enabled) {
        this.autoReconnectEnabled = enabled;
    }

    public boolean isAutoReconnectEnabled() {
        return autoReconnectEnabled;
    }

    // --- Device identity
    public void setDevice(String name, @Nullable String addr) {
        deviceName.setValue(name);
        deviceAddr = addr;
    }

    public LiveData<String> getDeviceName() {
        return deviceName;
    }

    @Nullable
    public String getDeviceAddr() {
        return deviceAddr;
    }

    // --- Connection state
    public LiveData<ConnectionState> getState() {
        return state;
    }

    public void setState(ConnectionState newState) {
        state.setValue(newState);
        // When leaving CONNECTED, if fake sensor is off, clear BPM to show "--"
        if (newState != ConnectionState.CONNECTED && !fakeSensorEnabled) {
            bpm.setValue(null);
        }
    }

    // --- BPM stream
    public LiveData<Integer> getBpm() {
        return bpm;
    }

    // --- Fake sensor controls
    public void setFakeSensorEnabled(boolean enabled) {
        if (enabled == fakeSensorEnabled) return;
        fakeSensorEnabled = enabled;
        if (enabled) {
            startFakeSensor();
        } else {
            stopFakeSensor();
        }
    }

    public boolean isFakeSensorEnabled() {
        return fakeSensorEnabled;
    }

    public void startFakeSensor() {
        if (fakeSensorEnabled) {
            handler.removeCallbacks(fakeTick);
            handler.post(fakeTick);
        }
    }

    public void stopFakeSensor() {
        fakeSensorEnabled = false;
        handler.removeCallbacks(fakeTick);
        bpm.setValue(null); // show "--"
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(fakeTick);
    }
}
