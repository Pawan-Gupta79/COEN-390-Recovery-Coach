package com.team11.smartgym.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SessionSummaryInstrumentedTest {

    private AppDb db;
    private SessionDao sessionDao;
    private SessionRepository repo;

    @Before
    public void createDb() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, AppDb.class)
                .allowMainThreadQueries()
                .build();
        sessionDao = db.sessionDao();
        repo = new SessionRepository(sessionDao);
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void finalizeSummary_matchesStoredSamples() {
        // 1) Create session
        long startMs = 1_000_000L;
        long sessionId = repo.createSession(startMs);

        // 2) Insert readings (simulate Fake button taps)
        List<Integer> bpms = Arrays.asList(72, 78, 90, 84, 96);
        long ts = startMs;
        for (int bpm : bpms) {
            Reading r = new Reading();
            r.sessionId = sessionId;
            r.timestamp = ts += 1000;  // 1s apart
            r.bpm = bpm;
            repo.insertReading(r);
        }

        // 3) Finalize (compute and persist)
        int expectedAvg = Math.round((72 + 78 + 90 + 84 + 96) / 5f); // = 84
        int expectedMax = 96;
        long endMs = startMs + 5_000L;
        repo.finalizeSession(sessionId, expectedAvg, expectedMax, endMs);

        // 4) Read back the session
        Session saved = sessionDao.getSessionById(sessionId);
        assertEquals(expectedAvg, saved.avgBpm);
        assertEquals(expectedMax, saved.maxBpm);
        assertTrue(saved.endedAt >= endMs);
        assertTrue(saved.startedAt <= saved.endedAt);
    }
}
