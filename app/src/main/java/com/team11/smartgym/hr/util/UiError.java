package com.team11.smartgym.hr.util;

public class UiError {
    public final String message;     // short, user-facing message
    public final String action;      // e.g., "Retry", "Open Settings"
    public final boolean isBlocking; // if true -> dialog; else -> snackbar

    public UiError(String message, String action, boolean isBlocking) {
        this.message = message;
        this.action = action;
        this.isBlocking = isBlocking;
    }
}
