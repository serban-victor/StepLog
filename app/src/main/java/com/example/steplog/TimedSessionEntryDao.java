package com.example.steplog;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TimedSessionEntryDao {

    @Insert
    void insert(TimedSessionEntry timedSessionEntry);

    @Query("SELECT * FROM timed_session_entries WHERE date = :date ORDER BY startTime DESC")
    List<TimedSessionEntry> getSessionsByDate(String date);

    @Query("DELETE FROM timed_session_entries")
    void deleteAllSessions();
}

