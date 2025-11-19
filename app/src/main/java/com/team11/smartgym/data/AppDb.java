package com.team11.smartgym.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * AppDb v2.
 *
 * Entities:
 *  - User
 *  - Session  (startedAt, endedAt, avgBpm, maxBpm)
 *  - Reading  (sessionId, timestamp, bpm)
 *
 * DS-06.x only added repository / buffering logic on top of this schema.
 * No new columns or tables were introduced, so no Room migration is required
 * beyond fallbackToDestructiveMigration() that is already configured.
 */
@Database(entities = {User.class, Session.class, Reading.class},
        version = 2,
        exportSchema = true)
public abstract class AppDb extends RoomDatabase {

    public abstract SessionDao sessionDao();
    public abstract UserDao userDao();

    private static volatile AppDb I;

    public static AppDb get(Context c) {
        if (I == null) {
            synchronized (AppDb.class) {
                if (I == null) {
                    I = Room.databaseBuilder(
                                    c.getApplicationContext(),
                                    AppDb.class,
                                    "smartgym.db")
                            // Sprint dev: allow main thread DB access to keep
                            // ViewModel / Fragment code simple.
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return I;
    }
}
