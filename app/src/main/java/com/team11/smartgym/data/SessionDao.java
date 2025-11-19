package com.team11.smartgym.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for Session + Reading.
 * Includes LiveData queries for UI.
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

    @Query("SELECT * FROM `Session` WHERE id = :id LIMIT 1")
    Session getSessionById(long id);

    @Query("DELETE FROM `Session`")
    int deleteAllSessions();

    @Query("SELECT * FROM `Session` ORDER BY startedAt DESC")
    List<Session> listSessions();

    @Query("SELECT * FROM `Session` ORDER BY startedAt DESC")
    LiveData<List<Session>> getAllSessionsLive();

    // ------- Workout CRUD (workout groups sessions) -------
    @Insert
    long insertWorkout(Workout w);

    @Update
    void updateWorkout(Workout w);

    @Query("SELECT * FROM `Workout` ORDER BY startedAt DESC")
    LiveData<List<Workout>> getAllWorkoutsLive();

    @Query("SELECT * FROM `Workout` WHERE id = :id LIMIT 1")
    androidx.lifecycle.LiveData<Workout> getWorkoutLiveById(long id);

    @Query("SELECT * FROM `Session` WHERE workoutId = :workoutId ORDER BY startedAt ASC")
    List<Session> listSessionsForWorkout(long workoutId);

    @Query("SELECT * FROM `Session` WHERE workoutId = :workoutId ORDER BY startedAt ASC")
    LiveData<List<Session>> getSessionsForWorkoutLive(long workoutId);

    @Query("SELECT w.id AS id, w.startedAt AS startedAt, w.endedAt AS endedAt, w.avgBpm AS avgBpm, w.maxBpm AS maxBpm, w.note AS note, (SELECT COUNT(*) FROM `Session` s WHERE s.workoutId = w.id) AS sessionCount FROM `Workout` w ORDER BY startedAt DESC")
    LiveData<List<WorkoutSummary>> getAllWorkoutSummariesLive();

    @Query("UPDATE `Workout` SET endedAt = :endedAt, avgBpm = :avg, maxBpm = :max WHERE id = :workoutId")
    int finalizeWorkout(long workoutId, long endedAt, int avg, int max);

    // ------- Reading CRUD -------
    @Insert
    void insertReading(Reading r);

    @Insert
    void insertReadings(List<Reading> readings);

    @Query("SELECT * FROM `Reading` WHERE sessionId = :sessionId ORDER BY `timestamp` ASC")
    List<Reading> getReadingsForSession(long sessionId);

    @Query("DELETE FROM `Reading` WHERE sessionId = :sessionId")
    int deleteReadingsForSession(long sessionId);

    // ------- Summary Finalization -------
    @Query("UPDATE `Session` SET endedAt = :endedAt, avgBpm = :avg, maxBpm = :max WHERE id = :sessionId")
    int finalizeSummary(long sessionId, long endedAt, int avg, int max);
}
