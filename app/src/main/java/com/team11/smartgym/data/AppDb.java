package com.team11.smartgym.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, Session.class, Reading.class}, version = 1)
public abstract class AppDb extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract UserDao userDao();
    private static volatile AppDb I;
    public static AppDb get(Context c) {
        if (I == null) {
            synchronized (AppDb.class) {
                if (I == null) I = Room.databaseBuilder(c.getApplicationContext(),
                        AppDb.class, "smartgym.db").build();
            }
        }
        return I;
    }
}