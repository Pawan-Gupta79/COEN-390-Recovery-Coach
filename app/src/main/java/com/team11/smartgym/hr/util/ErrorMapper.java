package com.team11.smartgym.hr.util;

import androidx.annotation.NonNull;

import com.team11.smartgym.R;
import com.team11.smartgym.hr.data.BleError;

/**
 * Central mapping from low-level BLE errors -> short, consistent UI copy.
 * Recoverable -> action "Retry" (snackbar/banner). Blocking -> dialog.
 */
public final class ErrorMapper {

    private ErrorMapper() {}

    public static @NonNull UiError from(BleError err) {
        if (err == null || err.code == null) {
            return copy(ErrorKeys.UNKNOWN);
        }
        switch (err.code) {
            case GATT_TIMEOUT:
                return copy(ErrorKeys.GATT_TIMEOUT);
            case DISCONNECTED:
                return copy(ErrorKeys.DEVICE_DISCONNECTED);
            case BLUETOOTH_OFF:
                return copy(ErrorKeys.BLUETOOTH_OFF);
            case PERMISSION_DENIED:
                return copy(ErrorKeys.PERMISSION_DENIED);
            case LOCATION_OFF:
                return copy(ErrorKeys.LOCATION_OFF);
            case CONNECT_FAILED:
                return copy(ErrorKeys.CONNECT_FAILED);
            case GATT_ERROR:
                return copy(ErrorKeys.GATT_ERROR);
            default:
                return copy(ErrorKeys.UNKNOWN);
        }
    }

    /** Single source of truth for short strings + blocking/recoverable. */
    private static UiError copy(ErrorKeys key) {
        switch (key) {
            case GATT_TIMEOUT:
                // Recoverable: stalled stream → let user retry
                return new UiError("Device not responding.", "Retry", false);
            case DEVICE_DISCONNECTED:
                return new UiError("Device disconnected.", "Retry", false);
            case BLUETOOTH_OFF:
                // Blocking: user must turn BT on
                return new UiError("Bluetooth is off. Turn it on to continue.", null, true);
            case PERMISSION_DENIED:
                return new UiError("Bluetooth permission is required.", null, true);
            case LOCATION_OFF:
                return new UiError("Location is off. Enable it to scan for devices.", null, true);
            case CONNECT_FAILED:
                return new UiError("Reconnect failed.", "Retry", false);
            case GATT_ERROR:
                return new UiError("Bluetooth error occurred.", "Retry", false);
            case UNKNOWN:
            default:
                return new UiError("Something went wrong.", "Retry", false);
        }
    }
}
