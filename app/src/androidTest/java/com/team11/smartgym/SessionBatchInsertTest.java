package com.team11.smartgym;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.team11.smartgym.data.AppDb;
import com.team11.smartgym.data.HrSampleBuffer;
import com.team11.smartgym.data.Reading;
import com.team11.smartgym.data.SessionDao;
import com.team11.smartgym.data.SessionRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Instrumented test for DS-06.2:
 *  - batch insert HR samples
 *  - ensure ordering by timestamp
 *  - ensure durability (samples actually go into DB)
 */
@RunWith(AndroidJUnit4.class)
public class SessionBatchInsertTest {

    private AppDb db;
    private SessionDao dao;
    private SessionRepository repo;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();

        db = Room.inMemoryDatabaseBuilder(ctx, AppDb.class)
                .allowMainThreadQueries() // test DB, safe here
                .build();

        dao = db.sessionDao();
        repo = new SessionRepository(dao);
    }

    @After
    public void teardown() throws IOException {
        db.close();
    }

    @Test
    public void testBatchInsert_threeFlushes() {
        // 1. Create a fake session ID
        long sessionId = repo.createSession(System.currentTimeMillis());

        // 2. Create buffer with maxBatchSize = 3
        HrSampleBuffer buf = new HrSampleBuffer(repo, sessionId, 3);

        long t0 = System.currentTimeMillis();

        // 3. Add samples (first 3 should auto-flush)
        buf.addSample(70, t0 + 1);
        buf.addSample(71, t0 + 2);
        buf.addSample(72, t0 + 3); // triggers flush #1

        buf.addSample(73, t0 + 4);
        buf.addSample(74, t0 + 5);
        // not flushed yet — flush manually
        buf.flush(); // flush #2

        // 4. Query DB
        List<Reading> list = repo.getReadings(sessionId);

        // ========== Assertions ==========

        // Total count should be 5
        assertEquals(5, list.size());

        // Values ordered by timestamp
        assertTrue(list.get(0).bpm == 70);
        assertTrue(list.get(1).bpm == 71);
        assertTrue(list.get(2).bpm == 72);
        assertTrue(list.get(3).bpm == 73);
        assertTrue(list.get(4).bpm == 74);

        // SessionId should match
        for (Reading r : list) {
            assertEquals(sessionId, r.sessionId);
        }
    }
}
