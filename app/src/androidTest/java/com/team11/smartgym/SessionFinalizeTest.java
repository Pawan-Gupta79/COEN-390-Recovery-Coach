package com.team11.smartgym;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.team11.smartgym.data.AppDb;
import com.team11.smartgym.data.HrSampleBuffer;
import com.team11.smartgym.data.Reading;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionDao;
import com.team11.smartgym.data.SessionRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * DS-06.3 - Verify that flushing + finalizeSessionFromReadings(...)
 * stores avgBpm, maxBpm, and endedAt correctly in the Session row.
 */
@RunWith(AndroidJUnit4.class)
public class SessionFinalizeTest {

    private AppDb db;
    private SessionDao dao;
    private SessionRepository repo;

    @Before
    public void setup() {
        Context ctx = ApplicationProvider.getApplicationContext();

        db = Room.inMemoryDatabaseBuilder(ctx, AppDb.class)
                .allowMainThreadQueries()   // OK for tests
                .build();

        dao = db.sessionDao();
        repo = new SessionRepository(dao);
    }

    @After
    public void tearDown() throws IOException {
        db.close();
    }

    @Test
    public void finalizeSessionFromReadings_setsAvgMaxAndEndedAt() {
        // 1. Create a session
        long startMs = 1_000L;
        long sessionId = repo.createSession(startMs);

        // 2. Stream some HR samples via HrSampleBuffer
        long baseTs = 2_000L;
        HrSampleBuffer buffer = new HrSampleBuffer(repo, sessionId, 10);

        buffer.addSample(70, baseTs + 1_000);  // t = 3000
        buffer.addSample(80, baseTs + 2_000);  // t = 4000
        buffer.addSample(90, baseTs + 3_000);  // t = 5000
        buffer.flush(); // ensure all samples are in DB

        // 3. Finalize session from readings
        repo.finalizeSessionFromReadings(sessionId);

        // 4. Reload Session row and assert summary
        Session s = dao.getSessionById(sessionId);

        // endedAt should be timestamp of last Reading
        assertEquals(baseTs + 3_000, s.endedAt);

        // avgBpm = (70 + 80 + 90) / 3 = 80
        assertEquals(80, s.avgBpm);

        // maxBpm = 90
        assertEquals(90, s.maxBpm);
    }
}
