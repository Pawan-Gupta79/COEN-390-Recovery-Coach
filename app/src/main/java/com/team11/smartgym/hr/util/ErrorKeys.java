package com.team11.smartgym.hr.util;

/** Stable keys for user-facing error copy. */
public enum ErrorKeys {
    GATT_TIMEOUT,           // sensor stalled / no response
    DEVICE_DISCONNECTED,    // link dropped unexpectedly
    BLUETOOTH_OFF,          // adapter off
    PERMISSION_DENIED,      // runtime permission missing
    LOCATION_OFF,           // required for scan on some Android versions
    CONNECT_FAILED,         // could not complete connect
    GATT_ERROR,             // generic GATT error
    UNKNOWN                 // fallback
}
