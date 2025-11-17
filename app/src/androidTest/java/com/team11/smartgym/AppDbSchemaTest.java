package com.team11.smartgym;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.team11.smartgym.data.AppDb;
import com.team11.smartgym.data.HrSampleBuffer;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionDao;
import com.team11.smartgym.data.SessionRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * DS-06.4 - Schema / migration sanity:
 *  - Can create AppDb v2.
 *  - Can persist Session + Reading rows with current schema.
 *  - finalizeSessionFromReadings(...) works against this schema.
 *
 * If we ever change the Room entities (add/remove fields), this test will run
 * against the new schema and fail if summaries can no longer be stored/read.
 */
@RunWith(AndroidJUnit4.class)
public class AppDbSchemaTest {

    private AppDb db;
    private SessionDao sessionDao;
    private SessionRepository repo;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();

        db = Room.inMemoryDatabaseBuilder(ctx, AppDb.class)
                .allowMainThreadQueries() // test DB
                .build();

        sessionDao = db.sessionDao();
        repo = new SessionRepository(sessionDao);
    }

    @After
    public void tearDown() throws IOException {
        db.close();
    }

    @Test
    public void sessionAndReadingSchema_supportsDs06Workflow() {
        // 1) Create a Session
        long startMs = 1_000L;
        long sessionId = repo.createSession(startMs);

        // 2) Stream readings via HrSampleBuffer
        long baseTs = 2_000L;
        HrSampleBuffer buffer = new HrSampleBuffer(repo, sessionId, 10);
        buffer.addSample(65, baseTs + 1_000); // 3000
        buffer.addSample(75, baseTs + 2_000); // 4000
        buffer.addSample(85, baseTs + 3_000); // 5000
        buffer.flush();

        // 3) Finalize session using current schema
        repo.finalizeSessionFromReadings(sessionId);

        // 4) Reload Session and assert summary fields are persisted correctly
        Session s = sessionDao.getSessionById(sessionId);

        // endedAt = last reading timestamp
        assertEquals(baseTs + 3_000, s.endedAt);

        // avgBpm = (65 + 75 + 85) / 3 = 75
        assertEquals(75, s.avgBpm);

        // maxBpm = 85
        assertEquals(85, s.maxBpm);
    }
}
