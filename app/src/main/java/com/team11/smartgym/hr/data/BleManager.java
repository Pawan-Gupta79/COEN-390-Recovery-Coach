package com.team11.smartgym.hr.data;

public interface BleManager {

    interface OnConnectionChangedListener {
        void onConnected();
        void onDisconnected();
    }

    void connect();
    void disconnect();

    void startHeartRate();
    void stopHeartRate();

}
