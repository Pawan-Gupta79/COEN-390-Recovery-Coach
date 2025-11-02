package com.team11.smartgym.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {
    private static final String FILE = "smart_gym_prefs";
    private static final String K_AUTO = "auto_reconnect";
    private static final String K_LAST_NAME = "last_device_name";
    private static final String K_LAST_ADDR = "last_device_addr";

    private final SharedPreferences sp;

    public AppPrefs(Context ctx) {
        sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // Auto-reconnect
    public boolean isAutoReconnect() {
        return sp.getBoolean(K_AUTO, false);
    }
    public void setAutoReconnect(boolean on) {
        sp.edit().putBoolean(K_AUTO, on).apply();
    }

    // Last device
    public String getLastDeviceName() { return sp.getString(K_LAST_NAME, ""); }
    public String getLastDeviceAddr() { return sp.getString(K_LAST_ADDR, ""); }

    public void setLastDevice(String name, String addr) {
        sp.edit()
                .putString(K_LAST_NAME, name == null ? "" : name)
                .putString(K_LAST_ADDR, addr == null ? "" : addr)
                .apply();
    }

    public void clearLastDevice() {
        sp.edit().remove(K_LAST_NAME).remove(K_LAST_ADDR).apply();
    }
}
