package com.team11.smartgym.data;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;

/**
 * Handles start/stop and live samples; persists summary on stop.
 */
public final class SessionController {

    private final SessionRepository repo;

    private Long currentSessionId = null;
    private long startMs = 0L;
    private final List<Integer> samples = new ArrayList<>();

    public SessionController(SessionRepository repo) {
        this.repo = repo;
    }

    public synchronized void startSession() {
        startMs = System.currentTimeMillis();
        currentSessionId = repo.createSession(startMs);
        samples.clear();
    }

    public synchronized void start() { startSession(); }

    public synchronized void addHeartRate(long ts, int bpm) {
        if (currentSessionId == null) return;
        int clamped = clampBpm(bpm);
        samples.add(clamped);

        Reading r = new Reading();
        r.sessionId = currentSessionId;
        r.timestamp = ts;
        r.bpm = clamped;
        repo.insertReading(r);
    }

    public synchronized void onHeartRate(int bpm) {
        addHeartRate(System.currentTimeMillis(), bpm);
    }

    public synchronized TempSessionSnapshot stopSessionAndReturnSnapshot() {
        if (currentSessionId == null) return null;

        long endMs = System.currentTimeMillis();
        Stats st = computeStats(samples);

        // Persist summary to DB
        repo.finalizeSession(currentSessionId, st.avg, st.max, endMs);

        TempSessionSnapshot snap = TempSessionSnapshot.of(
                startMs, endMs, samples.size(),
                new SessionStats(st.avg, st.max, st.invalid)
        );

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
