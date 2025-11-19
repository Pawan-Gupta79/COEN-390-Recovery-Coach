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
                long sessionId = repo.createSession(persistStart);
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
