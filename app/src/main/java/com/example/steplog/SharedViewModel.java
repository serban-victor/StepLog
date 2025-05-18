package com.example.steplog;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SharedViewModel extends AndroidViewModel {

    private MutableLiveData<Integer> stepsLiveData;
    private MutableLiveData<StepEntry> currentDayEntryLiveData;
    private MutableLiveData<List<StepEntry>> weeklyStepsLiveData;
    private AppDatabase db;
    private SharedPreferences prefs;

    // Constante
    private static final double METERS_PER_STEP_AVG = 0.78; // meters
    private static final double KM_PER_METER = 0.001;
    public static final double MILES_PER_KM = 0.621371;
    private static final double KCAL_PER_STEP_AVG = 0.04; // kcal
    private static final double KCAL_PER_STEP_PER_KG_DEFAULT_FACTOR = 0.0005; // kcal per step per kg (example)

    // Timer Fragment State
    private MutableLiveData<Boolean> isTimerRunningLiveData = new MutableLiveData<>(false);
    private MutableLiveData<Integer> timedStepCountLiveData = new MutableLiveData<>(0);
    private MutableLiveData<Long> chronometerBaseTimeLiveData = new MutableLiveData<>(SystemClock.elapsedRealtime());
    private MutableLiveData<Long> sessionStartTimeMillisLiveData = new MutableLiveData<>(0L);

    public SharedViewModel(Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        prefs = application.getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        stepsLiveData = new MutableLiveData<>();
        currentDayEntryLiveData = new MutableLiveData<>();
        weeklyStepsLiveData = new MutableLiveData<>();
    }

    public LiveData<Integer> getSteps() {
        return stepsLiveData;
    }

    public LiveData<StepEntry> getCurrentDayEntry() {
        return currentDayEntryLiveData;
    }

    public LiveData<List<StepEntry>> getWeeklySteps() {
        return weeklyStepsLiveData;
    }

    // Timer State LiveData Getters
    public LiveData<Boolean> getIsTimerRunning() {
        return isTimerRunningLiveData;
    }

    public LiveData<Integer> getTimedStepCount() {
        return timedStepCountLiveData;
    }

    public LiveData<Long> getChronometerBaseTime() {
        return chronometerBaseTimeLiveData;
    }
    public LiveData<Long> getSessionStartTimeMillis() {
        return sessionStartTimeMillisLiveData;
    }

    // Timer State Setters
    public void setTimerRunning(boolean isRunning) {
        isTimerRunningLiveData.setValue(isRunning);
    }

    public void setTimedStepCount(int steps) {
        timedStepCountLiveData.setValue(steps);
    }
    
    public void incrementTimedStepCount(int increment) {
        Integer currentSteps = timedStepCountLiveData.getValue();
        if (currentSteps == null) currentSteps = 0;
        timedStepCountLiveData.setValue(currentSteps + increment);
    }

    public void setChronometerBaseTime(long baseTime) {
        chronometerBaseTimeLiveData.setValue(baseTime);
    }
    
    public void setSessionStartTimeMillis(long startTime) {
        sessionStartTimeMillisLiveData.setValue(startTime);
    }

    public void resetTimerState() {
        isTimerRunningLiveData.setValue(false);
        timedStepCountLiveData.setValue(0);
        chronometerBaseTimeLiveData.setValue(SystemClock.elapsedRealtime());
        sessionStartTimeMillisLiveData.setValue(0L);
    }


    // Calculator distanta in KM
    public static double calculateDistanceKm(int steps, float userHeightCm) {
        double stepLengthMeters;
        if (userHeightCm > 0) {
            stepLengthMeters = (userHeightCm * 0.40) / 100.0; // Convert cm to m
        } else {
            stepLengthMeters = METERS_PER_STEP_AVG;
        }
        return (steps * stepLengthMeters) * KM_PER_METER;
    }

    // Calculatoo calorii
    public static double calculateCaloriesKcal(int steps, float userWeightKg) {
        double caloriesKcal;
        if (userWeightKg > 0) {
            caloriesKcal = steps * KCAL_PER_STEP_PER_KG_DEFAULT_FACTOR * userWeightKg;
        } else {
            caloriesKcal = steps * KCAL_PER_STEP_AVG;
        }
        return caloriesKcal;
    }

    public void setSteps(int steps, String date) {
        float userHeightCm = prefs.getFloat(UserFragment.KEY_HEIGHT, 0);
        float userWeightKg = prefs.getFloat(UserFragment.KEY_WEIGHT, 0);

        double distanceKm = calculateDistanceKm(steps, userHeightCm);
        double caloriesKcal = calculateCaloriesKcal(steps, userWeightKg);

        Log.d("StepLog_ViewModel", "setSteps: Date=" + date + ", Steps=" + steps + ", DistKM=" + distanceKm + ", CalKCAL=" + caloriesKcal);

        stepsLiveData.setValue(steps);

        new Thread(() -> {
            StepEntry entry = db.stepEntryDao().getEntryByDate(date);
            StepEntry entryToPost;
            if (entry == null) {
                entryToPost = new StepEntry(date, steps, distanceKm, caloriesKcal);
                db.stepEntryDao().insert(entryToPost);
                Log.d("StepLog_ViewModel", "New entry inserted for " + date);
            } else {
                entry.steps = steps;
                entry.distance = distanceKm;
                entry.calories = caloriesKcal;
                db.stepEntryDao().update(entry);
                entryToPost = entry;
                Log.d("StepLog_ViewModel", "Entry updated for " + date);
            }
            currentDayEntryLiveData.postValue(entryToPost);
            loadWeeklyStepsInBackground(); // Refresh weekly graph data
        }).start();
    }

    public void loadSteps(String date) {
        new Thread(() -> {
            StepEntry entry = db.stepEntryDao().getEntryByDate(date);
            if (entry != null) {
                stepsLiveData.postValue(entry.steps);
                currentDayEntryLiveData.postValue(entry);
            } else {
                stepsLiveData.postValue(0);
                currentDayEntryLiveData.postValue(new StepEntry(date, 0, 0, 0));
            }
        }).start();
    }

    public void loadWeeklySteps() {
        loadWeeklyStepsInBackground();
    }

    private void loadWeeklyStepsInBackground() {
        new Thread(() -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -6);
            String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            List<StepEntry> weeklyEntries = db.stepEntryDao().getEntriesForLastSevenDays(startDate);
            if (weeklyEntries == null) {
                weeklyStepsLiveData.postValue(Collections.emptyList());
            } else {
                weeklyStepsLiveData.postValue(weeklyEntries);
            }
        }).start();
    }
}

