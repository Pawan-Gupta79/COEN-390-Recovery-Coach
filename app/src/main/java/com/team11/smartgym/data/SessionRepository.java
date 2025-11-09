package com.team11.smartgym.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Repository layer that wraps DAO operations.
 * Adds LiveData stream for non-blocking UI reads.
 */
public class SessionRepository {

    private final AppDatabase db;

    public SessionRepository(Context context) {
        this.db = DatabaseProvider.get(context);
    }

    // ---------------------------------------------------------
    // SESSION CREATION (called on Start)
    // ---------------------------------------------------------
    public long createSession(long startMs) {
        Session s = new Session();
        s.startedAt = startMs;
        s.endedAt = 0;
        s.avgBpm = 0;
        s.maxBpm = 0;
        return db.sessionDao().insertSession(s);
    }

    // ---------------------------------------------------------
    // ADD HR SAMPLE (during session)
    // ---------------------------------------------------------
    public void addReading(long sessionId, int bpm) {
        Reading r = new Reading();
        r.sessionId = sessionId;
        r.timestamp = System.currentTimeMillis();
        r.bpm = bpm;
        db.sessionDao().insertReading(r);
    }

    // ---------------------------------------------------------
    // FINALIZE SESSION (avg/max + end time)
    // ---------------------------------------------------------
    public void finalizeSession(long sessionId, int avg, int max, long endMs) {
        db.sessionDao().finalizeSummary(sessionId, endMs, avg, max);
    }

    // ---------------------------------------------------------
    // QUERIES
    // ---------------------------------------------------------
    public Session getSession(long id) {
        return db.sessionDao().getSessionById(id);
    }

    public List<Session> getAllSessions() {
        return db.sessionDao().listSessions();
    }

    /** 🔹 Stream for UI: observe sessions without blocking main thread. */
    public LiveData<List<Session>> observeSessions() {
        return db.sessionDao().observeSessions();
    }

    public List<Reading> getReadingsForSession(long sessionId) {
        return db.sessionDao().getReadingsForSession(sessionId);
    }

    // ---------------------------------------------------------
    // DELETE OPS (used rarely)
    // ---------------------------------------------------------
    public void deleteAllSessions() {
        db.sessionDao().deleteAllSessions();
    }
}
