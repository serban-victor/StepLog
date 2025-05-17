// AppDatabase.java
package com.example.steplog;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {StepEntry.class, TimedSessionEntry.class}, version = 2) // Added TimedSessionEntry and incremented version
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract StepEntryDao stepEntryDao();
    public abstract TimedSessionEntryDao timedSessionEntryDao(); // Added DAO for TimedSessionEntry

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "step_database" // numele bazei de date
                            )
                            .fallbackToDestructiveMigration() // recreează baza dacă schimbă versiunea
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

