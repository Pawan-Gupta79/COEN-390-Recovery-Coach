package com.team11.smartgym.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Individual heart-rate sample tied to a Session (FK).
 */
@Entity(
        tableName = "hr_samples",
        foreignKeys = @ForeignKey(
                entity = Session.class,
                parentColumns = "id",
                childColumns = "sessionId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("sessionId"),
                @Index("timestampMs")
        }
)
public class HrSample {
    @PrimaryKey(autoGenerate = true) public long id;

    /** FK → sessions.id */
    public long sessionId;

    /** Epoch ms for this sample. */
    public long timestampMs;

    /** Beats per minute (already clamped in controller). */
    public int bpm;
}
