package com.team11.smartgym.ble;

import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Heart-rate spike filter + simple debounce.
 *
 * Responsibilities:
 *  - Drop obviously invalid values (<30 or >230 bpm).
 *  - Suppress crazy jumps compared to last stable value.
 *  - Smooth the signal using a rolling average window.
 *
 * Usage:
 *    HeartRateFilter f = new HeartRateFilter();
 *    Integer clean = f.addSample(rawBpm);   // may return null
 */
public class HeartRateFilter {

    // ---- Documented thresholds ----
    // Drop values that are clearly impossible for humans.
    public static final int MIN_VALID_BPM = 30;
    public static final int MAX_VALID_BPM = 230;

    // Maximum allowed jump vs last stable sample.
    // Larger jumps are treated as transient spikes.
    public static final int MAX_JUMP_DELTA = 40;  // bpm

    // Size of rolling window for smoothing.
    public static final int DEFAULT_WINDOW_SIZE = 5;

    private final int windowSize;
    private final Deque<Integer> window = new ArrayDeque<>();

    // Last accepted (stable) BPM after filtering.
    private Integer lastStable;

    public HeartRateFilter() {
        this(DEFAULT_WINDOW_SIZE);
    }

    public HeartRateFilter(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        this.windowSize = windowSize;
    }

    /**
     * Add a raw BPM sample and get the filtered/smoothed value.
     *
     * @param rawBpm Raw BPM from HeartRateParser (may be null or negative).
     * @return Filtered BPM, or null if no stable value is available yet.
     */
    @Nullable
    public synchronized Integer addSample(@Nullable Integer rawBpm) {
        // Treat null / non-positive as "no data"
        if (rawBpm == null || rawBpm <= 0) {
            window.clear();
            // Do NOT reset lastStable; we keep it as "last known good"
            return lastStable;
        }

        // 1) Hard bounds – ignore impossible values
        if (rawBpm < MIN_VALID_BPM || rawBpm > MAX_VALID_BPM) {
            // Ignore spike; keep previous stable value
            return lastStable;
        }

        // 2) Jump filter – suppress sudden huge jumps vs last stable
        if (lastStable != null && Math.abs(rawBpm - lastStable) > MAX_JUMP_DELTA) {
            // Treat as spike; ignore
            return lastStable;
        }

        // 3) Rolling window smoothing
        window.addLast(rawBpm);
        if (window.size() > windowSize) {
            window.removeFirst();
        }

        int sum = 0;
        for (int v : window) {
            sum += v;
        }
        int smoothed = sum / window.size();

        lastStable = smoothed;
        return smoothed;
    }

    /**
     * Clear smoothing window and last stable value.
     */
    public synchronized void reset() {
        window.clear();
        lastStable = null;
    }

    @Nullable
    public synchronized Integer getLastStable() {
        return lastStable;
    }
}
