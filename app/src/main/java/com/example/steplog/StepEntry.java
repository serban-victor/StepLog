package com.example.steplog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "step_entries")
public class StepEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String date; // Format: "yyyy-MM-dd"
    public int steps;
    public double distance; // în kilometri
    public double calories; // kcal

    public StepEntry(String date, int steps, double distance, double calories) {
        this.date = date;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
    }
}
