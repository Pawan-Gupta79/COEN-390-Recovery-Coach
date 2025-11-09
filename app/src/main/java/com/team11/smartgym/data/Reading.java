package com.team11.smartgym.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "Reading",
        foreignKeys = @ForeignKey(
                entity = Session.class,
                parentColumns = "id",
                childColumns = "sessionId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("sessionId"),
                @Index("timestamp")
        }
)
public class Reading {
    @PrimaryKey(autoGenerate = true) public long id;
    public long sessionId;
    /** Epoch ms for this sample (matches DAO queries) */
    public long timestamp;
    public int bpm;
}
