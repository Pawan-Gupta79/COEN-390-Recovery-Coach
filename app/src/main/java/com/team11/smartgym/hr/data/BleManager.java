package com.team11.smartgym.hr.data;

public interface BleManager {

    interface OnConnectionChangedListener {
        void onConnected();
        void onDisconnected();
    }

    interface OnHeartRateListener {
        void onHeartRate(int value);
    }

    interface OnErrorListener {
        void onError(BleError error);
    }

    // Connection
    void connect();
    void disconnect();
    boolean isConnected();

    // HR notifications
    void startHeartRate();
    void stopHeartRate();

    // Set listeners
    void setConnectionListener(OnConnectionChangedListener listener);
    void setHeartRateListener(OnHeartRateListener listener);
    void setErrorListener(OnErrorListener listener);
}
