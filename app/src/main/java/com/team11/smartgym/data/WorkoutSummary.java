package com.team11.smartgym.data;

/**
 * Projection used by Room to show workout list with session counts.
 * Room will map query columns into this POJO by matching field names.
 */
public class WorkoutSummary {
    public long id;
    public long startedAt;
    public long endedAt;
    public int avgBpm;
    public int maxBpm;
    public String note;
    public int sessionCount;

    public WorkoutSummary() {}
}
