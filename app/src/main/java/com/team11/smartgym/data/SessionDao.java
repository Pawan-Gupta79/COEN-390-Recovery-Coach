package com.team11.smartgym.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for sessions + HR readings.
 * Includes both synchronous and LiveData queries.
 */
@Dao
public interface SessionDao {

    // ------------------------- SESSION CRUD -------------------------

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

    @Query("SELECT * FROM Session ORDER BY startedAt DESC")
    List<Session> listSessions();

    // ---------- LiveData versions (required by SessionRepository) ----------

    @Query("SELECT * FROM Session ORDER BY startedAt DESC")
    LiveData<List<Session>> getAllSessionsLive();

    @Query("SELECT * FROM Session WHERE id = :id LIMIT 1")
    LiveData<Session> getSessionLive(long id);


    // ------------------------- READING CRUD -------------------------

    @Insert
    void insertReading(Reading r);

    @Insert
    void insertReadings(List<Reading> readings);

    @Query("SELECT * FROM Reading WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<Reading> getReadingsForSession(long sessionId);

    @Query("DELETE FROM Reading WHERE sessionId = :sessionId")
    int deleteReadingsForSession(long sessionId);


    // ---------------- Session finalization (summary) ----------------

    @Query("UPDATE Session SET endedAt = :endedAt, avgBpm = :avgBpm, maxBpm = :maxBpm WHERE id = :sessionId")
    int finalizeSummary(long sessionId, long endedAt, int avgBpm, int maxBpm);
}
