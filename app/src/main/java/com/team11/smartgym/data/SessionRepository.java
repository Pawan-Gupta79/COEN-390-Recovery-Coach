package com.team11.smartgym.data;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository bridging DAOs and SessionController/ViewModels.
 * Provides async write operations + LiveData read operations.
 */
public class SessionRepository {

    private final SessionDao sessionDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public SessionRepository(SessionDao dao) {
        this.sessionDao = dao;
    }

    // ---------------------------------------------------------
    //  CREATE SESSION  (required by SessionController)
    // ---------------------------------------------------------
    /**
     * Create a new Session row with start timestamp only.
     * Returns the DB-assigned sessionId.
     */
    public synchronized long createSession(long startMs) {
        Session s = new Session();
        s.startedAt = startMs;
        s.endedAt = 0;
        s.avgBpm = 0;
        s.maxBpm = 0;

        return sessionDao.insertSession(s);
    }

    // ---------------------------------------------------------
    //  FINALIZE SUMMARY  (avg/max/end time)
    // ---------------------------------------------------------
    public void finalizeSession(long sessionId, int avg, int max, long endMs) {
        io.execute(() -> sessionDao.finalizeSummary(sessionId, endMs, avg, max));
    }

    // ---------------------------------------------------------
    //  READ OPERATIONS
    // ---------------------------------------------------------
    public LiveData<List<Session>> getAllSessionsLive() {
        return sessionDao.getAllSessionsLive();
    }

    public LiveData<Session> getSessionLive(long id) {
        return sessionDao.getSessionLive(id);
    }

    // ---------------------------------------------------------
    //  INSERT HR READINGS
    // ---------------------------------------------------------
    public void insertReading(Reading r) {
        io.execute(() -> sessionDao.insertReading(r));
    }

    // ---------------------------------------------------------
    //  ADVICE ENGINE (DS-07.3)
    // ---------------------------------------------------------
    public LiveData<String> getAdviceForSession(long sessionId) {
        return androidx.lifecycle.Transformations.map(
                sessionDao.getSessionLive(sessionId),
                session -> {
                    if (session == null) return "NO SESSION";

                    // Sprint 3 will compute actual RMSSD/LF/HF
                    Double rmssd = null;
                    Double lfHf = null;

                    return AdviceEngine.computeAdvice(rmssd, lfHf);
                }
        );
    }
}
