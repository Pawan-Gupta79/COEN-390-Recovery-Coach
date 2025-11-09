package com.team11.smartgym.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SessionStatsUtilTest {

    @Test
    public void empty_returnsInvalid() {
        List<SessionStatsUtil.TimestampedBpm> samples = new ArrayList<>();
        SessionStats stats = SessionStatsUtil.compute(samples, now(), 4000);
        assertTrue(stats.invalid);
        assertEquals(0, stats.averageBpm);
        assertEquals(0, stats.maxBpm);
    }

    @Test
    public void allIgnored_returnsInvalid() {
        long start = now();
        List<SessionStatsUtil.TimestampedBpm> samples = new ArrayList<>();
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 1000, 80));
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 2000, 85));
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 3000, 90));

        SessionStats stats = SessionStatsUtil.compute(samples, start, 4000);
        assertTrue(stats.invalid);
    }

    @Test
    public void normal_computesAvgAndMax() {
        long start = now();
        List<SessionStatsUtil.TimestampedBpm> samples = new ArrayList<>();
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 1000, 100));
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 3500, 110));
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 4000, 120));
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 5000, 130));
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 6000, 140));

        SessionStats stats = SessionStatsUtil.compute(samples, start, 4000);
        assertFalse(stats.invalid);
        assertEquals(130, stats.averageBpm);
        assertEquals(140, stats.maxBpm);
    }

    @Test
    public void negativesClampedToZero() {
        long start = now();
        List<SessionStatsUtil.TimestampedBpm> samples = new ArrayList<>();
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 4500, -10));
        samples.add(new SessionStatsUtil.TimestampedBpm(start + 5000, 90));

        SessionStats stats = SessionStatsUtil.compute(samples, start, 4000);
        assertFalse(stats.invalid);
        assertEquals(45, stats.averageBpm);
        assertEquals(90, stats.maxBpm);
    }

    private static long now() { return System.currentTimeMillis(); }
}
