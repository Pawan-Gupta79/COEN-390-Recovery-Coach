package com.team11.smartgym.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/** Central session helper (single source of truth). */
public class SessionManager {

    private static final String FILE = "smart_gym_session";
    private static final String KEY_LOGGED_IN_EMAIL = "logged_in_email";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Returns true if a user email is stored. */
    public boolean isLoggedIn() {
        return prefs.contains(KEY_LOGGED_IN_EMAIL);
    }

    /** Persist logged-in user email (or any non-empty value). */
    public void setLoggedIn(String email) {
        prefs.edit().putString(KEY_LOGGED_IN_EMAIL, email == null ? "" : email).apply();
    }

    /** Clear session completely. */
    public void clear() {
        prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply();
    }

    @Nullable
    public String getEmail() {
        String v = prefs.getString(KEY_LOGGED_IN_EMAIL, null);
        return (v != null && v.isEmpty()) ? null : v;
        // null means not logged in
    }
}
