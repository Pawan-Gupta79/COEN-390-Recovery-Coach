package com.team11.smartgym.debug;

class WorkoutSession {
    long durationMs;
    long steps;
    float avgHr;
    float calories;

    WorkoutSession(long durationMs, long steps, float avgHr, float calories) {
        this.durationMs = durationMs;
        this.steps = steps;
        this.avgHr = avgHr;
        this.calories = calories;
    }
}
