package com.team11.smartgym.data;

import java.io.Serializable;
import java.util.UUID;

/** Temporary snapshot used at session stop. NOT a Room entity. */
public final class TempSessionSnapshot implements Serializable {

    public final String tempId;
    public final long startMs;
    public final long endMs;
    public final int samplesCount;
    public final SessionStats stats;

    // PUBLIC constructor (required for JSON restore)
    public TempSessionSnapshot(String tempId,
                               long startMs,
                               long endMs,
                               int samplesCount,
                               SessionStats stats) {
        this.tempId = tempId;
        this.startMs = startMs;
        this.endMs = endMs;
        this.samplesCount = samplesCount;
        this.stats = stats;
    }

    // Factory method (for creating snapshots at STOP event)
    public static TempSessionSnapshot of(long startMs,
                                         long endMs,
                                         int samplesCount,
                                         SessionStats stats) {
        String id = UUID.randomUUID().toString();
        return new TempSessionSnapshot(id, startMs, endMs, samplesCount, stats);
    }

    @Override
    public String toString() {
        return "TempSessionSnapshot{" +
                "tempId='" + tempId + '\'' +
                ", startMs=" + startMs +
                ", endMs=" + endMs +
                ", samplesCount=" + samplesCount +
                ", stats=" + stats +
                '}';
    }
}
