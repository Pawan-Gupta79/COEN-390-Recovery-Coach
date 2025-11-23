package com.team11.smartgym.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Session")
public class Session {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String deviceName;
    public String workoutType;

    public long startedAt;      // epoch ms
    public long endedAt;        // epoch ms

    public int avgBpm;
    public int maxBpm;
}