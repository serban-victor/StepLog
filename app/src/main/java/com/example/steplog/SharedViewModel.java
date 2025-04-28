package com.example.steplog;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SharedViewModel extends AndroidViewModel {

    private MutableLiveData<Integer> stepsLiveData;
    private AppDatabase db;

    public SharedViewModel(Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        stepsLiveData = new MutableLiveData<>();
    }

    // Funcție pentru a obține pașii salvați
    public LiveData<Integer> getSteps() {
        return stepsLiveData;
    }

    // Funcție pentru a actualiza pașii în ViewModel
    public void setSteps(int steps, String date) {
        // Calcul distanță și calorii
        double distance = steps * 0.0008; // 0.0008 km per pas
        double calories = steps * 0.04; // 0.04 kcal per pas

        // Log pentru a verifica pașii salvați
        Log.d("StepLog", "Salvăm pași pentru data: " + date + " cu " + steps + " pași.");

        // Actualizează pașii în ViewModel
        stepsLiveData.setValue(steps);

        // Salvează pașii în baza de date
        new Thread(() -> {
            StepEntry entry = db.stepEntryDao().getEntryByDate(date);
            if (entry == null) {
                // Dacă nu există intrare pentru data respectivă, o creăm
                StepEntry newEntry = new StepEntry(date, steps, distance, calories);
                db.stepEntryDao().insert(newEntry);
                Log.d("StepLog", "Pași salvați: " + steps + " pe data " + date);
            } else {
                // Dacă există, actualizăm intrarea
                entry.steps = steps;
                entry.distance = distance;
                entry.calories = calories;
                db.stepEntryDao().update(entry);
                Log.d("StepLog", "Pași actualizați: " + steps + " pe data " + date);
            }
        }).start();
    }

    // Încărcarea pașilor din baza de date
    public void loadSteps(String date) {
        new Thread(() -> {
            StepEntry entry = db.stepEntryDao().getEntryByDate(date);
            if (entry != null) {
                stepsLiveData.postValue(entry.steps);
            } else {
                stepsLiveData.postValue(0); // Dacă nu există intrare pentru data respectivă
            }
        }).start();
    }
}
