package com.team11.smartgym.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/** Room DB: declare entities + DAO getters. */
@Database(
        entities = {
                User.class,
                Session.class,
                Reading.class   // we use Reading (your sample entity), not “HrSample”
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract SessionDao sessionDao();
}
