package com.team11.smartgym.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = @Index("sessionId"))
public class Reading {
    @PrimaryKey(autoGenerate = true) public long id;
    public long sessionId;
    public long ts;
    public int bpm;
}