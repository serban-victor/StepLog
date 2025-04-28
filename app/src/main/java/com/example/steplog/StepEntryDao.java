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
}
