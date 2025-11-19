package com.team11.smartgym.data;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Thin repository around SessionDao.
 * (allowMainThreadQueries() is enabled in DatabaseProvider for Sprint dev)
 */
public class SessionRepository {

    private final SessionDao sessionDao;

    public SessionRepository(SessionDao dao) {
        this.sessionDao = dao;
    }

    // ---------- Live list for UI ----------
    public LiveData<List<Session>> getAllSessions() {
        return sessionDao.getAllSessionsLive();
    }

    // ---------- Session lifecycle ----------
    public long createSession(long startMs) {
        Session s = new Session();
        s.startedAt = startMs;
        s.endedAt = 0L;
        s.avgBpm = 0;
        s.maxBpm = 0;
        return sessionDao.insertSession(s);
    }

    public void finalizeSession(long sessionId, int avg, int max, long endedAt) {
        sessionDao.finalizeSummary(sessionId, endedAt, avg, max);
    }

    public void updateSession(Session s) {
        sessionDao.updateSession(s);
    }

    // ---------- Readings ----------
    public void insertReading(Reading r) {
        sessionDao.insertReading(r);
    }

    public List<Reading> getReadings(long sessionId) {
        return sessionDao.getReadingsForSession(sessionId);
    }
}
