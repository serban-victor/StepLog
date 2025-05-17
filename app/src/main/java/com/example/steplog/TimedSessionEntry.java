package com.example.steplog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "timed_session_entries")
public class TimedSessionEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String date; // Format: YYYY-MM-DD
    public long startTime; // Timestamp
    public long endTime; // Timestamp
    public long durationMillis;
    public int steps;
    public double distanceKm;
    public double caloriesKcal;

    public TimedSessionEntry(String date, long startTime, long endTime, long durationMillis, int steps, double distanceKm, double caloriesKcal) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMillis = durationMillis;
        this.steps = steps;
        this.distanceKm = distanceKm;
        this.caloriesKcal = caloriesKcal;
    }
}

