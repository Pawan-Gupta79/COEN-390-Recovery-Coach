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

    // ---------- Workouts ----------
    public LiveData<List<Workout>> getAllWorkoutsLive() {
        return sessionDao.getAllWorkoutsLive();
    }

    public androidx.lifecycle.LiveData<Workout> getWorkoutLiveById(long id) {
        return sessionDao.getWorkoutLiveById(id);
    }

    public LiveData<java.util.List<WorkoutSummary>> getAllWorkoutSummariesLive() {
        return sessionDao.getAllWorkoutSummariesLive();
    }
    // ---------- Session lifecycle ----------
    public long createSession(long startMs) {
        return createSession(startMs, null, null);
    }

    /** Create a new Session row and optionally attach it to a workout. */
    public long createSession(long startMs, Long workoutId, String type) {
        Session s = new Session();
        s.startedAt = startMs;
        s.endedAt = 0L;
        s.avgBpm = 0;
        s.maxBpm = 0;
        s.workoutId = workoutId;
        s.type = type;
        return sessionDao.insertSession(s);
    }

    public void finalizeSession(long sessionId, int avg, int max, long endedAt) {
        sessionDao.finalizeSummary(sessionId, endedAt, avg, max);
    }

    // ---------- Workout lifecycle ----------
    public long createWorkout(long startMs) {
        Workout w = new Workout();
        w.startedAt = startMs;
        w.endedAt = 0L;
        w.avgBpm = 0;
        w.maxBpm = 0;
        // create workout with no per-workout activity type; activity type is stored per-Session
        return sessionDao.insertWorkout(w);
    }

    public void finalizeWorkout(long workoutId, long endedAt, int avg, int max) {
        sessionDao.finalizeWorkout(workoutId, endedAt, avg, max);
    }

    // activity type is stored per-session; no updateWorkoutType

    public void deleteWorkoutCascade(long workoutId) {
        sessionDao.deleteWorkoutCascade(workoutId);
    }

    public java.util.List<Session> listSessionsForWorkout(long workoutId) {
        return sessionDao.listSessionsForWorkout(workoutId);
    }

    public LiveData<List<Session>> getSessionsForWorkoutLive(long workoutId) {
        return sessionDao.getSessionsForWorkoutLive(workoutId);
    }

    public Session getSessionById(long id) {
        return sessionDao.getSessionById(id);
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
