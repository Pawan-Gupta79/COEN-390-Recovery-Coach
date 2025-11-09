package com.team11.smartgym.data;

import android.content.Context;

import androidx.room.Room;

/**
 * Central place to provide:
 * - AppDatabase singleton
 * - SessionRepository singleton
 * - SessionController singleton
 *
 * ViewModels call DatabaseProvider.get(context)
 * to access the controllers & repositories.
 */
public final class DatabaseProvider {

    private static DatabaseProvider INSTANCE;

    private final AppDatabase db;
    private final SessionRepository sessionRepo;
    private final SessionController sessionController;

    private DatabaseProvider(Context appContext) {

        db = Room.databaseBuilder(appContext, AppDatabase.class, "smartgym.db")
                .fallbackToDestructiveMigration()
                .build();

        sessionRepo = new SessionRepository(db.sessionDao());
        sessionController = new SessionController(sessionRepo);
    }

    /** Static accessor for ViewModels. Always call with APPLICATION context. */
    public static synchronized DatabaseProvider get(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseProvider(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    // ---- Exposed getters ----

    public AppDatabase getDb() {
        return db;
    }

    public SessionRepository getSessionRepository() {
        return sessionRepo;
    }

    public SessionController getSessionController() {
        return sessionController;
    }
}
