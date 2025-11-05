package com.team11.smartgym.hr.data;

public class BleError {
    public enum Code {
        GATT_TIMEOUT,
        DISCONNECTED,
        BLUETOOTH_OFF,
        PERMISSION_DENIED,
        LOCATION_OFF,
        CONNECT_FAILED,
        GATT_ERROR
    }

    public final Code code;
    public final String detail; // optional diagnostic

    public BleError(Code code) { this(code, null); }
    public BleError(Code code, String detail) {
        this.code = code;
        this.detail = detail;
    }
}

