package com.example.steplog;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StepEntryDao {
    @Insert
    void insert(StepEntry entry); //introduce un rand nou in step_entries

    @Query("SELECT * FROM step_entries WHERE date = :date LIMIT 1")
    StepEntry getEntryByDate(String date);
    //cauta o intrare cu o anumita data
    @Query("SELECT * FROM step_entries")
    List<StepEntry> getAllEntries();

    @Update
    void update(StepEntry entry); // Metoda pentru actualizarea intrarii existente

    // Query pentru graficul din HomeFragment
    @Query("SELECT * FROM step_entries WHERE date >= :startDate ORDER BY date ASC LIMIT 7")
    List<StepEntry> getEntriesForLastSevenDays(String startDate);

    @Query("DELETE FROM step_entries")
    void deleteAllEntries(); // pentru reset din setari
}

