package com.team11.smartgym.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SessionDao {
    @Insert long insertSession(Session s);
    @Update void updateSession(Session s);

    @Insert void insertReading(Reading r);

    @Query("SELECT * FROM Session ORDER BY startedAt DESC")
    List<Session> listSessions();
}