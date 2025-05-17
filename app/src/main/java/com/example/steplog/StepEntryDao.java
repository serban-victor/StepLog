package com.example.steplog;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StepEntryDao {
    @Insert
    void insert(StepEntry entry);

    @Query("SELECT * FROM step_entries WHERE date = :date LIMIT 1")
    StepEntry getEntryByDate(String date);

    @Query("SELECT * FROM step_entries")
    List<StepEntry> getAllEntries();

    @Update
    void update(StepEntry entry); // Metoda pentru actualizarea intrării existente

    // New query to get steps for the last 7 days for the graph
    @Query("SELECT * FROM step_entries WHERE date >= :startDate ORDER BY date ASC LIMIT 7")
    List<StepEntry> getEntriesForLastSevenDays(String startDate);

    @Query("DELETE FROM step_entries")
    void deleteAllEntries(); // Added for data reset functionality
}

