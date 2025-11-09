package com.team11.smartgym.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;

/**
 * SessionController
 * - Start: resets buffers and marks start time
 * - Add samples: feed incoming BPM during the live session
 * - Stop: computes stats (ignoring the first N ms), saves TempSessionSnapshot via TempSessionStore,
 *         then resets buffers (carry-over logic for DS-01.7)
 *
 * Notes (DS-01.8):
 * - We clamp BPM samples to [0, 220] to avoid skew from impossible spikes.
 * - Ignore-window is configurable via setIgnoreWindowMs().
 * - No DB writes here (Sprint 3 will handle Room).
 */
public final class SessionController {

    /** Milliseconds to ignore at session start due to unstable readings (3–5s typical). */
    private long ignoreWindowMs = 4000L; // default 4s

    /** Hard ceiling for plausible BPM to mitigate sensor spikes. */
    private static final int MAX_BPM = 220;

    private boolean running = false;
    private long startMs = 0L;

    private final List<Sample> samples = new ArrayList<>();
    private final TempSessionStore store;

    public SessionController(Context context) {
        this.store = new TempSessionStore(context.getApplicationContext());
    }

    // ---------- Lifecycle -------------

    /** Start a new live session (resets the buffers). */
    public synchronized void start() {
        running = true;
        startMs = System.currentTimeMillis();
        samples.clear();
    }

    /**
     * Add a BPM sample with the current time as timestamp.
     * Safe to call frequently (e.g., from BLE callbacks).
     */
    public synchronized void addSample(int bpm) {
        if (!running) return;
        long t = System.currentTimeMillis();
        samples.add(new Sample(t, clampBpm(bpm)));
    }

    /**
     * Stop the session:
     *  - compute stats ignoring the first {@link #ignoreWindowMs} milliseconds
     *  - create TempSessionSnapshot
     *  - save to TempSessionStore
     *  - reset buffers (carry-over requirement)
     *
     * @return the saved TempSessionSnapshot, or null if session wasn't running
     */
    public synchronized TempSessionSnapshot stopAndSave() {
        if (!running) return null;

        long endMs = System.currentTimeMillis();
        Stats stats = computeStats(samples, startMs, ignoreWindowMs);

        TempSessionSnapshot snapshot = TempSessionSnapshot.of(
                startMs,
                endMs,
                samples.size(),
                new SessionStats(stats.avg, stats.max, stats.invalid)
        );

        store.save(snapshot);

        // Reset buffers after saving (carry-over requirement)
        running = false;
        startMs = 0L;
        samples.clear();

        return snapshot;
    }

    // ---------- Helpers -------------

    private static Stats computeStats(List<Sample> samples, long startMs, long ignoreMs) {
        if (samples.isEmpty() || startMs <= 0) {
            return new Stats(0, 0, true);
        }
        final long cutoff = startMs + Math.max(0, ignoreMs);

        IntSummaryStatistics s = samples.stream()
                .filter(x -> x.timestampMs >= cutoff)
                .mapToInt(x -> x.bpm) // already clamped on insert
                .summaryStatistics();

        if (s.getCount() == 0) {
            return new Stats(0, 0, true);
        }
        int avg = (int) Math.round(s.getAverage());
        int max = s.getMax();
        return new Stats(avg, max, false);
    }

    private static int clampBpm(int bpm) {
        if (bpm < 0) return 0;
        if (bpm > MAX_BPM) return MAX_BPM;
        return bpm;
    }

    // ---------- Public utilities ----------

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized long getStartMs() {
        return startMs;
    }

    public void setIgnoreWindowMs(long ignoreWindowMs) {
        if (ignoreWindowMs < 0) ignoreWindowMs = 0;
        this.ignoreWindowMs = ignoreWindowMs;
    }

    /** Optional: read back the last saved snapshot (for your Sessions Fragment). */
    public TempSessionSnapshot loadLastSnapshot() {
        return store.load();
    }

    /** Optional: read back all temp snapshots (oldest→newest). */
    public List<TempSessionSnapshot> loadAllSnapshots() {
        return store.loadAll();
    }

    /** Optional: clear the last snapshot and history. */
    public void clearLastSnapshot() {
        store.clear();
    }

    // ---------- Internal types ----------

    private static final class Sample {
        final long timestampMs;
        final int bpm;
        Sample(long t, int bpm) {
            this.timestampMs = t;
            this.bpm = bpm;
        }
    }

    private record Stats(int avg, int max, boolean invalid) {}
}
