package com.team11.smartgym.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "smartgym_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(String email) {
        sp.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public boolean isLoggedIn() {
        return sp.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getEmail() {
        return sp.getString(KEY_EMAIL, "");
    }

    public void logout() {
        sp.edit().clear().apply();
    }
}
