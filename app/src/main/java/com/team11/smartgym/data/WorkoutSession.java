package com.team11.smartgym.data;

public class WorkoutSession {
    private final long id;
    private final String deviceName;
    private final long startedAt;
    private final long endedAt;
    private final int averageBpm;
    private final int maxBpm;
    private final int durationSeconds;

    public WorkoutSession(long id, String deviceName, long startedAt, long endedAt, int averageBpm, int maxBpm, int durationSeconds) {
        this.id = id;
        this.deviceName = deviceName;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.averageBpm = averageBpm;
        this.maxBpm = maxBpm;
        this.durationSeconds = durationSeconds;
    }

    public long getId() { return id; }
    public String getDeviceName() { return deviceName; }
    public long getStartedAt() { return startedAt; }
    public long getEndedAt() { return endedAt; }
    public int getAverageBpm() { return averageBpm; }
    public int getMaxBpm() { return maxBpm; }
    public int getDurationSeconds() { return durationSeconds; }
}
