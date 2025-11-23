package com.team11.smartgym.data;

import java.util.IntSummaryStatistics;
import java.util.List;

/**
 * Pure utility to compute average/max BPM while ignoring an initial unstable window.
 * Also clamps BPM into a physiological range [0, 220] to avoid skew from outliers/noise.
 */
public final class SessionStatsUtil {

    private SessionStatsUtil() {}

    /** Hard ceiling for plausible BPM. */
    private static final int MAX_BPM = 220;

    /**
     * Compute stats from timestamped BPM samples, ignoring the first ignoreMs after startMs.
     *
     * @param samples   list of (timestampMs, bpm) samples
     * @param startMs   session start (epoch ms)
     * @param ignoreMs  milliseconds to ignore from the start for unstable readings
     * @return          SessionStats with avg/max/invalid
     */
    public static SessionStats compute(List<TimestampedBpm> samples, long startMs, long ignoreMs) {
        if (samples == null || samples.isEmpty() || startMs <= 0) {
            return new SessionStats(0, 0, true);
        }
        final long cutoff = startMs + Math.max(0, ignoreMs);

        IntSummaryStatistics s = samples.stream()
                .filter(x -> x.timestampMs >= cutoff)
                .mapToInt(x -> clampBpm(x.bpm))
                .summaryStatistics();

        if (s.getCount() == 0) {
            return new SessionStats(0, 0, true);
        }
        int avg = (int) Math.round(s.getAverage());
        int max = s.getMax();
        return new SessionStats(avg, max, false);
    }

    /** Clamp BPM to [0, MAX_BPM]. */
    private static int clampBpm(int bpm) {
        if (bpm < 0) return 0;
        if (bpm > MAX_BPM) return MAX_BPM;
        return bpm;
    }

    /** Minimal immutable sample used for tests and utilities. */
    public static final class TimestampedBpm {
        public final long timestampMs;
        public final int bpm;

        public TimestampedBpm(long timestampMs, int bpm) {
            this.timestampMs = timestampMs;
            this.bpm = bpm;
        }
    }
}
