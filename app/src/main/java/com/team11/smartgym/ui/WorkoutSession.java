package com.team11.smartgym.model;

/**
 * Model representing a completed workout session. created by pawan
 */
public class WorkoutSession {
    private final long id;
    private final String deviceName;
    private final long startedAt;
    private final long endedAt;
    private final int avgHeartRate;
    private final int maxHeartRate;
    private final int duration; // in seconds

    private final String heartRate;

    public WorkoutSession(long id, String deviceName, long startedAt, long endedAt,
                          int avgHeartRate, int maxHeartRate, int duration) {
        this.id = id;
        this.deviceName = deviceName;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.avgHeartRate = avgHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.duration = duration;
        this.heartRate = "";
    }
    public WorkoutSession(long id, String deviceName, long startedAt, long endedAt,
                          int avgHeartRate, int maxHeartRate, int duration, String heartRate) {
        this.id = id;
        this.deviceName = deviceName;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.avgHeartRate = avgHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.duration = duration;
        this.heartRate = heartRate;                                                                 //saves a string of heartrate "bpm1, bpm2, bpm3, ..."
    }

    public long getId() {
        return id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public int getAvgHeartRate() {
        return avgHeartRate;
    }

    public int getMaxHeartRate() {
        return maxHeartRate;
    }

    public int getDuration() {
        return duration;
    }

    /**
     * Returns duration formatted as "MM:SS"
     */
    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}