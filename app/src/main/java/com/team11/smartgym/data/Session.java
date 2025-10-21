package com.team11.smartgym.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Session {
    @PrimaryKey(autoGenerate = true) public long id;
    public long startedAt;
    public long endedAt; // 0 until finished
    public int avgBpm;
    public int maxBpm;
}