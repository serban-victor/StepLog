package com.example.steplog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepSaveReceiver extends BroadcastReceiver {

    private AppDatabase db;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Obține pașii salvați
        int steps = intent.getIntExtra("steps", 0);
        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());

        // Calculator distanta si calorii
        double distance = steps * 0.78 / 1000.0; // presupunând că un pas = 0.78 metri
        double calories = steps * 0.04; // 0.04 kcal / pas

        // Formatăm data pentru a o salva în baza de date
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));

        // Salveaza pasii in baza de date
        db = AppDatabase.getInstance(context);
        StepEntry entry = new StepEntry(date, steps, distance, calories);

        new Thread(() -> {
            db.stepEntryDao().insert(entry);
            Log.d("StepSaveReceiver", "Pași salvați pentru data " + date + ": " + steps);
        }).start();
    }
}