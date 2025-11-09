package com.team11.smartgym.data;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;

/**
 * SessionController
 * - Start: create a Session row (via repository) and remember its ID.
 * - onHeartRate: record live BPM samples, forward each to the repository as a Reading.
 * - Stop: compute avg/max, finalize the Session summary in DB, and return a carry-over snapshot.
 *
 * Keeps a simple in-memory samples list so we can compute avg/max on stop.
 */
public final class SessionController {

    private final SessionRepository repo;

    // Active session state
    private Long currentSessionId = null;
    private long startMs = 0L;
    private final List<Integer> samples = new ArrayList<>();

    public SessionController(SessionRepository repo) {
        this.repo = repo;
    }

    /** Begin a new session: create DB row and reset counters. */
    public synchronized void startSession() {
        startMs = System.currentTimeMillis();
        currentSessionId = repo.createSession(startMs);
        samples.clear();
    }

    /** Convenience used by ViewModel if it wants a shorter name. */
    public synchronized void start() { startSession(); }

    /**
     * Add a live HR sample (timestamp+bpm). Called by BLE sink.
     * Clamps bpm and forwards to repository as a Reading.
     */
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

    /** Optional: if some caller only has bpm, we timestamp it here. */
    public synchronized void onHeartRate(int bpm) {
        addHeartRate(System.currentTimeMillis(), bpm);
    }

    /**
     * Stop the session, compute stats, finalize DB summary,
     * and return a TempSessionSnapshot for carry-over UI.
     */
    public synchronized TempSessionSnapshot stopSessionAndReturnSnapshot() {
        if (currentSessionId == null) return null;

        long endMs = System.currentTimeMillis();
        Stats stats = computeStats(samples);

        // Finalize DB summary
        repo.finalizeSession(currentSessionId, stats.avg, stats.max, endMs);

        // Build a temp snapshot for carry-over UI
        TempSessionSnapshot snap = TempSessionSnapshot.of(
                startMs,
                endMs,
                samples.size(),
                new SessionStats(stats.avg, stats.max, stats.invalid)
        );

        // Reset active state
        currentSessionId = null;
        startMs = 0L;
        samples.clear();

        return snap;
    }

    /** Convenience used by ViewModel if it wants a shorter name. */
    public synchronized TempSessionSnapshot stopAndSave() {
        return stopSessionAndReturnSnapshot();
    }

    // ---------------- Helpers ----------------

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
        int avg = (int)Math.round(s.getAverage());
        int max = s.getMax();
        return new Stats(avg, max, false);
    }

    private record Stats(int avg, int max, boolean invalid) {}
}
