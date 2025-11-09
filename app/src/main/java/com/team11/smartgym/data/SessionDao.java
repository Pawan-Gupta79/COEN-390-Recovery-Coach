package com.team11.smartgym.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for Session + Reading (your existing HR sample entity).
 * Extended to expose LiveData stream for non-blocking UI.
 */
@Dao
public interface SessionDao {

    // ------- Session CRUD -------
    @Insert
    long insertSession(Session s);

    @Update
    void updateSession(Session s);

    @Delete
    void deleteSession(Session s);

    @Query("SELECT * FROM Session WHERE id = :id LIMIT 1")
    Session getSessionById(long id);

    @Query("DELETE FROM Session")
    int deleteAllSessions();

    // Synchronous list (OK for background threads)
    @Query("SELECT * FROM Session ORDER BY startedAt DESC")
    List<Session> listSessions();

    // 🔹 Non-blocking stream for UI (meets acceptance #2)
    @Query("SELECT * FROM Session ORDER BY startedAt DESC")
    LiveData<List<Session>> observeSessions();


    // ------- Reading CRUD (your existing HR sample entity) -------
    @Insert
    void insertReading(Reading r);

    @Insert
    void insertReadings(List<Reading> readings);

    @Query("SELECT * FROM Reading WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<Reading> getReadingsForSession(long sessionId);

    @Query("DELETE FROM Reading WHERE sessionId = :sessionId")
    int deleteReadingsForSession(long sessionId);


    // ------- Summary Finalization (avg/max/end) -------
    @Query("UPDATE Session SET endedAt = :endedAt, avgBpm = :avg, maxBpm = :max WHERE id = :sessionId")
    int finalizeSummary(long sessionId, long endedAt, int avg, int max);
}
