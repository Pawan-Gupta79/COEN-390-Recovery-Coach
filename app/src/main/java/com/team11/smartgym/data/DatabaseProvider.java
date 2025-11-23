package com.team11.smartgym.data;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides singletons for:
 *  - AppDb (canonical Room database)
 *  - SessionRepository
 *  - SessionController
 *
 * Dev note: allowMainThreadQueries() enabled for Sprint dev to avoid thread crashes.
 * TODO [Sprint 3]: move DB writes to background (Executors) and remove allowMainThreadQueries().
 */
public final class DatabaseProvider {

    private static DatabaseProvider INSTANCE;

    private final AppDb db;
    private final SessionRepository sessionRepo;
    private final SessionController sessionController;
    private final ExecutorService dbExecutor;

    private DatabaseProvider(Context appContext) {
        db = AppDb.get(appContext);

        // single-threaded executor for all DB writes to keep ordering predictable
        dbExecutor = Executors.newSingleThreadExecutor();

        sessionRepo = new SessionRepository(db.sessionDao());
        sessionController = new SessionController(sessionRepo, dbExecutor);
    }

    /** Always pass application context. */
    public static synchronized DatabaseProvider get(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseProvider(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    public AppDb getDb() { return db; }
    public SessionRepository getSessionRepository() { return sessionRepo; }
    public SessionController getSessionController() { return sessionController; }
    public ExecutorService getDbExecutor() { return dbExecutor; }
}
