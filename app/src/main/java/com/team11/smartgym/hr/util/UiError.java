package com.team11.smartgym.hr.util;

public class UiError {

    public UiError(String message, String action, boolean isBlocking) {
        this.message = message;
        this.action = action;
        this.isBlocking = isBlocking;
    }
}
