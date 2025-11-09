package com.team11.smartgym.data;

import android.content.Context;
import java.util.List;

public class SessionRepo {
    private final AppDb db;
    public SessionRepo(Context c) { db = AppDb.get(c); }

    public long startSession() {
        Session s = new Session();
        s.startedAt = System.currentTimeMillis();
        s.avgBpm = 0; s.maxBpm = 0; s.endedAt = 0;
        return db.sessionDao().insertSession(s);
    }

    public void addReading(long sessionId, int bpm) {
        Reading r = new Reading();
        r.sessionId = sessionId;
        r.timestamp = System.currentTimeMillis();
        r.bpm = bpm;
    }

    public void endSession(long id, int avg, int max) {
        Session s = new Session();
        s.id = id;
        s.endedAt = System.currentTimeMillis();
        s.avgBpm = avg; s.maxBpm = max;
        db.sessionDao().updateSession(s);
    }

    public List<Session> list() { return db.sessionDao().listSessions(); }
}