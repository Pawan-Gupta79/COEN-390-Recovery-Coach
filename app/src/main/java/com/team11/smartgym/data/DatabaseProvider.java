package com.team11.smartgym.data;

import android.content.Context;
import androidx.room.Room;

/** Thread-safe singleton for AppDatabase. */
public final class DatabaseProvider {
    private static volatile AppDatabase INSTANCE;

    private DatabaseProvider() {}

    public static AppDatabase get(Context context) {
        AppDatabase local = INSTANCE;
        if (local == null) {
            synchronized (DatabaseProvider.class) {
                local = INSTANCE;
                if (local == null) {
                    local = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "smartgym.db"
                            )
                            // DEV ONLY: drop/regen on schema changes. Replace with proper migrations next sprint.
                            .fallbackToDestructiveMigration()
                            .build();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }
}
