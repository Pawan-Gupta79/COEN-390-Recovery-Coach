package com.team11.smartgym.hr.util;

/** One-shot UI error event for either dialog (blocking) or snackbar/banner (recoverable). */
public class UiError {
    public final String message;
    public final String action;   // e.g., "Retry" (null if no action)
    public final boolean isBlocking;

    public UiError(String message, String action, boolean isBlocking) {
        this.message = message;
        this.action = action;
        this.isBlocking = isBlocking;
    }
}
