package com.team11.smartgym.shared;

import android.content.Context;
import android.content.Intent;

public class Bus {
    public static final String ACTION_HR_UPDATE = "com.team11.smartgym.HR";
    public static final String ACTION_STATE = "com.team11.smartgym.STATE";
    public static final String ACTION_ERROR = "com.team11.smartgym.ERROR";

    public static final String EXTRA_BPM = "bpm";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_ERROR = "error";

    public static void sendHr(Context c, int bpm) {
        c.sendBroadcast(new Intent(ACTION_HR_UPDATE).putExtra(EXTRA_BPM, bpm));
    }
    public static void sendState(Context c, String s) {
        c.sendBroadcast(new Intent(ACTION_STATE).putExtra(EXTRA_STATE, s));
    }
    public static void sendError(Context c, String e) {
        c.sendBroadcast(new Intent(ACTION_ERROR).putExtra(EXTRA_ERROR, e));
    }
}