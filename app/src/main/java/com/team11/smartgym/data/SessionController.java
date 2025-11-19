package com.team11.smartgym.data;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Handles start/stop and live samples; persists summary on stop.
 */
public final class SessionController {

    private final SessionRepository repo;

    private Long currentSessionId = null;
    private long startMs = 0L;
    private final List<Integer> samples = new ArrayList<>();
    private final ExecutorService dbExecutor;
    private Long currentWorkoutId = null;

    public Long getCurrentWorkoutId() { return currentWorkoutId; }

    /** Start a workout by scheduling its creation on the DB executor. */
    public synchronized void startWorkout(long workoutStartMs) {
        currentWorkoutId = null;
        dbExecutor.execute(() -> {
            try {
                long wid = repo.createWorkout(workoutStartMs);
                currentWorkoutId = wid;
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Start a workout synchronously: submit creation to the DB executor and wait for the inserted id.
     * This blocks the calling thread until the DB insert completes and returns the new workout id,
     * and also updates `currentWorkoutId`.
     * Use carefully from the UI thread (the operation is fast but will block until insert finishes).
     */
    public synchronized long startWorkoutSync(long workoutStartMs) {
        currentWorkoutId = null;
        try {
            java.util.concurrent.Callable<Long> task = () -> repo.createWorkout(workoutStartMs);
            java.util.concurrent.Future<Long> f = dbExecutor.submit(task);
            Long wid = f.get();
            if (wid != null) currentWorkoutId = wid;
            return wid == null ? -1L : wid;
        } catch (Exception e) {
            return -1L;
        }
    }

    /** End the current workout by computing summary from sessions and persisting. */
    public synchronized void endWorkout(long endedAt) {
        final Long wid = currentWorkoutId;
        if (wid == null) return;
        dbExecutor.execute(() -> {
            try {
                // Compute simple summary: average of session.avgBpm and max of session.maxBpm
                java.util.List<Session> sessions = repo.listSessionsForWorkout(wid);
                if (sessions == null || sessions.isEmpty()) {
                    repo.finalizeWorkout(wid, endedAt, 0, 0);
                    return;
                }
                int sum = 0;
                int max = 0;
                int count = 0;
                long lastEnd = endedAt;
                for (Session s : sessions) {
                    sum += s.avgBpm;
                    if (s.maxBpm > max) max = s.maxBpm;
                    if (s.endedAt > lastEnd) lastEnd = s.endedAt;
                    count++;
                }
                int avg = count == 0 ? 0 : sum / count;
                repo.finalizeWorkout(wid, lastEnd, avg, max);
            } catch (Exception ignored) {}
        });
        currentWorkoutId = null;
    }

    /**
     * Recompute and persist the workout summary (avg/max/endedAt) from stored sessions.
     * This does not clear `currentWorkoutId` — use when adding sessions to an ongoing workout.
     */
    public synchronized void recomputeWorkoutSummary(Long wid) {
        if (wid == null) return;
        dbExecutor.execute(() -> recomputeWorkoutSummarySync(wid));
    }

    /**
     * Synchronous variant that computes and persists the workout summary on the current thread.
     * Call this only from a background thread (e.g. the DB executor) to avoid blocking the UI.
     */
    public void recomputeWorkoutSummarySync(long wid) {
        try {
            java.util.List<Session> sessions = repo.listSessionsForWorkout(wid);
            if (sessions == null || sessions.isEmpty()) {
                repo.finalizeWorkout(wid, 0L, 0, 0);
                return;
            }
            int sum = 0;
            int max = 0;
            long lastEnd = 0L;
            int count = 0;
            for (Session s : sessions) {
                sum += s.avgBpm;
                if (s.maxBpm > max) max = s.maxBpm;
                if (s.endedAt > lastEnd) lastEnd = s.endedAt;
                count++;
            }
            int avg = count == 0 ? 0 : sum / count;
            repo.finalizeWorkout(wid, lastEnd, avg, max);
        } catch (Exception ignored) {}
    }

    public SessionController(SessionRepository repo, ExecutorService dbExecutor) {
        this.repo = repo;
        this.dbExecutor = dbExecutor;
    }

    public synchronized void startSession() {
        // Start in-memory session; persist only on stop.
        startMs = System.currentTimeMillis();
        currentSessionId = null;
        samples.clear();
    }

    public synchronized void start() { startSession(); }

    public synchronized void addHeartRate(long ts, int bpm) {
        int clamped = clampBpm(bpm);
        samples.add(clamped);
    }

    public synchronized void onHeartRate(int bpm) {
        addHeartRate(System.currentTimeMillis(), bpm);
    }

    public synchronized TempSessionSnapshot stopSessionAndReturnSnapshot() {
        if (samples == null) return null;

        long endMs = System.currentTimeMillis();
        Stats st = computeStats(samples);

        // Prepare snapshot to return immediately
        TempSessionSnapshot snap = TempSessionSnapshot.of(
                startMs, endMs, samples.size(),
                new SessionStats(st.avg, st.max, st.invalid)
        );

        // Persist in background: create session, insert readings, finalize
        final long persistStart = startMs;
        final long persistEnd = endMs;
        final int avg = st.avg;
        final int max = st.max;
        final List<Integer> readingsCopy = new ArrayList<>(samples);

        dbExecutor.execute(() -> {
            try {
                long sessionId = repo.createSession(persistStart, currentWorkoutId);
                for (Integer bpm : readingsCopy) {
                    Reading r = new Reading();
                    r.sessionId = sessionId;
                    r.timestamp = System.currentTimeMillis(); // approximate
                    r.bpm = bpm;
                    repo.insertReading(r);
                }
                repo.finalizeSession(sessionId, avg, max, persistEnd);
            } catch (Exception ignored) {
                // swallow: repo may be unavailable in some test scenarios
            }
        });

        currentSessionId = null;
        startMs = 0L;
        samples.clear();

        return snap;
    }

    public synchronized TempSessionSnapshot stopAndSave() {
        return stopSessionAndReturnSnapshot();
    }

    private static int clampBpm(int bpm) {
        if (bpm < 0) return 0;
        if (bpm > 220) return 220;
        return bpm;
    }

    private static Stats computeStats(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new Stats(0, 0, true);
        }
        IntSummaryStatistics s = values.stream().mapToInt(i -> i).summaryStatistics();
        int avg = (int) Math.round(s.getAverage());
        int max = s.getMax();
        return new Stats(avg, max, false);
    }

    private record Stats(int avg, int max, boolean invalid) {}
}
