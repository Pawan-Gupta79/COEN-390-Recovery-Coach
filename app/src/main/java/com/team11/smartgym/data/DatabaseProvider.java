package com.team11.smartgym.data;

import android.content.Context;

import androidx.room.Room;

/**
 * Provides singletons for:
 *  - AppDatabase
 *  - SessionRepository
 *  - SessionController
 *
 * Dev note: allowMainThreadQueries() enabled for Sprint dev to avoid thread crashes.
 * TODO [Sprint 3]: move DB writes to background (Executors) and remove allowMainThreadQueries().
 */
public final class DatabaseProvider {

    private static DatabaseProvider INSTANCE;

    private final AppDatabase db;
    private final SessionRepository sessionRepo;
    private final SessionController sessionController;

    private DatabaseProvider(Context appContext) {
        db = Room.databaseBuilder(appContext, AppDatabase.class, "smartgym.db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()   // ✅ quick unblock for Start/Stop flow
                .build();

        sessionRepo = new SessionRepository(db.sessionDao());
        sessionController = new SessionController(sessionRepo);
    }

    /** Always pass application context. */
    public static synchronized DatabaseProvider get(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseProvider(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    public AppDatabase getDb() { return db; }
    public SessionRepository getSessionRepository() { return sessionRepo; }
    public SessionController getSessionController() { return sessionController; }
}
