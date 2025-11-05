package com.team11.smartgym.hr.util;

import com.team11.smartgym.hr.domain.ConnectionState;

public class ErrorMapper {

    public static UiError from(Throwable t) {
        // Defaults
        String msg = "Something went wrong";
        String action = "Retry";
        boolean blocking = false;

        if (t == null) return new UiError(msg, action, blocking);

        String name = t.getClass().getSimpleName();
        String text = t.getMessage() != null ? t.getMessage() : "";

        String lower = text.toLowerCase();

        if (name.contains("Gatt") || lower.contains("gatt")) {
            msg = "Bluetooth connection lost";
            action = "Retry";
        } else if (name.contains("Timeout") || lower.contains("timeout")) {
            msg = "Device not responding";
            action = "Retry";
        } else if (name.contains("Permission") || lower.contains("permission")) {
            msg = "Bluetooth permission required";
            action = "Open Settings";
            blocking = true;
        } else if (name.contains("Unsupported") || lower.contains("unsupported")) {
            msg = "Device unsupported";
            action = "OK";
            blocking = true;
        }

        return new UiError(msg, action, blocking);
    }

    public static UiError fromState(ConnectionState state) {
        if (state == ConnectionState.ERROR) {
            return new UiError("Unable to read heart rate", "Retry", false);
        }
        return null;
    }
}
