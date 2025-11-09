package com.team11.smartgym.data;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SessionDaoInstrumentedTest {

    private AppDatabase db;
    private SessionDao sessionDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries() // fine for tests
                .build();
        sessionDao = db.sessionDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void insertAndReadBackSession() {
        Session s = new Session();
        s.startedAt = 1111L;
        s.endedAt = 0L;
        s.avgBpm = 0;
        s.maxBpm = 0;

        long id = sessionDao.insertSession(s);
        assertTrue(id > 0);

        Session fromDb = sessionDao.getSessionById(id);
        assertNotNull(fromDb);
        assertEquals(1111L, fromDb.startedAt);
    }

    @Test
    public void insertReadingsAndQueryBySession() {
        // create a session
        Session s = new Session();
        s.startedAt = 1234L;
        long sessionId = sessionDao.insertSession(s);

        // insert a couple of readings
        Reading r1 = new Reading();
        r1.sessionId = sessionId; r1.timestamp = 2000L; r1.bpm = 90;
        sessionDao.insertReading(r1);

        Reading r2 = new Reading();
        r2.sessionId = sessionId; r2.timestamp = 3000L; r2.bpm = 110;
        sessionDao.insertReading(r2);

        List<Reading> list = sessionDao.getReadingsForSession(sessionId);
        assertEquals(2, list.size());
        assertEquals(90, list.get(0).bpm);
        assertEquals(110, list.get(1).bpm);
    }
}
