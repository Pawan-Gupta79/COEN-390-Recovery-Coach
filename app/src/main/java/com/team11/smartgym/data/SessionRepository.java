package com.team11.smartgym.data;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Thin repository around SessionDao.
 * (allowMainThreadQueries() is enabled in AppDb for Sprint dev)
 *
 * DS-06.x:
 *  - DS-06.1: createSession(...) is the entry point for starting a new session.
 *  - DS-06.2: readings are attached to a sessionId via insertReading(s)/HrSampleBuffer.
 *  - DS-06.3: finalizeSessionFromReadings(...) computes summary stats and updates Session.
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

    /**
     * DS-06.1 - Create a new Session row and return its generated ID.
     *
     * The insert is atomic at the DB level via Room's @Insert.
     *
     * @param startMs epoch time in ms when the session starts
     * @return generated session ID
     */
    public long createSession(long startMs) {
        Session s = new Session();
        s.startedAt = startMs;
        s.endedAt = 0L;
        s.avgBpm = 0;
        s.maxBpm = 0;
        return sessionDao.insertSession(s);
    }

    /**
     * DS-06.3 - Finalize a session given precomputed stats.
     */
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

    /**
     * DS-06.2 - Batch insert readings for streaming HR data.
     * Each Reading must already have its sessionId and timestamp set.
     */
    public void insertReadings(List<Reading> readings) {
        sessionDao.insertReadings(readings);
    }

    public List<Reading> getReadings(long sessionId) {
        return sessionDao.getReadingsForSession(sessionId);
    }

    // ---------- DS-06.3: summary from readings ----------

    /**
     * DS-06.3
     * Compute avg / max BPM and endedAt from all Reading rows for this session,
     * then update the Session row via finalizeSummary(...).
     *
     * endedAt is taken as the timestamp of the last Reading (chronological order).
     * If there are no readings, endedAt falls back to System.currentTimeMillis()
     * and avg/max are set to 0.
     */
    public void finalizeSessionFromReadings(long sessionId) {
        List<Reading> readings = sessionDao.getReadingsForSession(sessionId);
        if (readings == null || readings.isEmpty()) {
            long now = System.currentTimeMillis();
            sessionDao.finalizeSummary(sessionId, now, 0, 0);
            return;
        }

        int sum = 0;
        int max = Integer.MIN_VALUE;
        long lastTs = readings.get(readings.size() - 1).timestamp;

        for (Reading r : readings) {
            sum += r.bpm;
            if (r.bpm > max) {
                max = r.bpm;
            }
        }

        int avg = sum / readings.size();

        sessionDao.finalizeSummary(sessionId, lastTs, avg, max);
    }
}
