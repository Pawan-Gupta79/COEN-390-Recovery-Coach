package com.team11.smartgym.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * Thin wrapper around SharedPreferences for app-wide flags and session.
 */
public class AppPrefs {

    private static final String FILE = "smart_gym_prefs";

    // Session
    private static final String KEY_LOGGED_IN = "logged_in";

    // Auto-reconnect
    private static final String KEY_AUTO_RECONNECT = "auto_reconnect";

    // Last device
    private static final String KEY_LAST_DEVICE_NAME = "last_device_name";
    private static final String KEY_LAST_DEVICE_ADDR = "last_device_addr";

    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---------- Session ----------
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    /** Clears only the login flag (used by Logout). */
    public void clearLogin() {
        prefs.edit().remove(KEY_LOGGED_IN).apply();
    }

    // ---------- Auto-reconnect ----------
    public boolean isAutoReconnect() {
        return prefs.getBoolean(KEY_AUTO_RECONNECT, true);
    }

    public void setAutoReconnect(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply();
    }

    // ---------- Last device ----------
    public void setLastDevice(String name, @Nullable String addr) {
        prefs.edit()
                .putString(KEY_LAST_DEVICE_NAME, name == null ? "" : name)
                .putString(KEY_LAST_DEVICE_ADDR, addr == null ? "" : addr)
                .apply();
    }

    public String getLastDeviceName() {
        return prefs.getString(KEY_LAST_DEVICE_NAME, "");
    }

    @Nullable
    public String getLastDeviceAddr() {
        String v = prefs.getString(KEY_LAST_DEVICE_ADDR, "");
        return (v == null || v.isEmpty()) ? null : v;
    }

    public void clearLastDevice() {
        prefs.edit()
                .remove(KEY_LAST_DEVICE_NAME)
                .remove(KEY_LAST_DEVICE_ADDR)
                .apply();
    }
}
