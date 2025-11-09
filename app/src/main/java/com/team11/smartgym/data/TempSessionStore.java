package com.team11.smartgym.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Temp persistence for recently finished sessions (no DB).
 * Now stores a rolling history (max 20) + still exposes "last" for compatibility.
 */
public final class TempSessionStore {

    private static final String PREF_FILE = "smartgym_temp_store";
    private static final String KEY_LAST_SESSION = "last_session";
    private static final String KEY_HISTORY = "session_history";
    private static final int MAX_HISTORY = 20;

    private final SharedPreferences sp;

    public TempSessionStore(Context context) {
        this.sp = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    /** Save snapshot and append to history (rolling). */
    public void save(TempSessionSnapshot snap) {
        if (snap == null) return;
        try {
            JSONObject obj = toJson(snap);
            // Save "last"
            sp.edit().putString(KEY_LAST_SESSION, obj.toString()).apply();

            // Append to history
            JSONArray history = getHistoryArray();
            history.put(obj);
            // Trim if above max
            if (history.length() > MAX_HISTORY) {
                JSONArray trimmed = new JSONArray();
                // keep the last MAX_HISTORY elements
                int start = history.length() - MAX_HISTORY;
                for (int i = start; i < history.length(); i++) {
                    trimmed.put(history.opt(i));
                }
                history = trimmed;
            }
            sp.edit().putString(KEY_HISTORY, history.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    /** Load last snapshot (or null). */
    public TempSessionSnapshot load() {
        String json = sp.getString(KEY_LAST_SESSION, null);
        if (json == null || json.isEmpty()) return null;
        try {
            return fromJson(new JSONObject(json));
        } catch (JSONException e) {
            return null;
        }
    }

    /** Load all snapshots in chronological order (oldest → newest). */
    public List<TempSessionSnapshot> loadAll() {
        ArrayList<TempSessionSnapshot> out = new ArrayList<>();
        JSONArray arr = getHistoryArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) continue;
            try {
                out.add(fromJson(obj));
            } catch (JSONException ignored) {
            }
        }
        return out;
    }

    /** Clear both last and history. */
    public void clear() {
        sp.edit().remove(KEY_LAST_SESSION).remove(KEY_HISTORY).apply();
    }

    // ---------------- JSON helpers --------------------

    private JSONArray getHistoryArray() {
        String raw = sp.getString(KEY_HISTORY, null);
        if (raw == null || raw.isEmpty()) return new JSONArray();
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static JSONObject toJson(TempSessionSnapshot s) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("tempId", s.tempId);
        root.put("startMs", s.startMs);
        root.put("endMs", s.endMs);
        root.put("samplesCount", s.samplesCount);

        JSONObject stats = new JSONObject();
        stats.put("averageBpm", s.stats.averageBpm);
        stats.put("maxBpm", s.stats.maxBpm);
        stats.put("invalid", s.stats.invalid);

        root.put("stats", stats);
        return root;
    }

    private static TempSessionSnapshot fromJson(JSONObject root) throws JSONException {
        String id = root.optString("tempId", null);
        long startMs = root.optLong("startMs", 0L);
        long endMs = root.optLong("endMs", 0L);
        int samplesCount = root.optInt("samplesCount", 0);

        JSONObject statsObj = root.optJSONObject("stats");
        int averageBpm = 0;
        int maxBpm = 0;
        boolean invalid = false;
        if (statsObj != null) {
            averageBpm = statsObj.optInt("averageBpm", 0);
            maxBpm = statsObj.optInt("maxBpm", 0);
            invalid = statsObj.optBoolean("invalid", false);
        }

        SessionStats stats = new SessionStats(averageBpm, maxBpm, invalid);
        return new TempSessionSnapshot(id, startMs, endMs, samplesCount, stats);
    }
}
