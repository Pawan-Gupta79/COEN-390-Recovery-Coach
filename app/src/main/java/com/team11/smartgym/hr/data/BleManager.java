package com.team11.smartgym.hr.data;

public interface BleManager {

    interface OnHeartRateListener {
        void onHeartRate(int bpm);
    }

    interface OnErrorListener {
        void onError(Throwable error);
    }

    interface OnConnectionChangedListener {
        void onConnected();
        void onDisconnected();
    }

    // --- Connection control ---
    void connect();
    void disconnect();

    // --- Heart-rate notifications ---
    void startHeartRate();
    void stopHeartRate();

    // --- State ---
    boolean isConnected();

    // --- Listeners ---
    void setHeartRateListener(OnHeartRateListener l);
    void setErrorListener(OnErrorListener l);
    void setConnectionListener(OnConnectionChangedListener l);
}
