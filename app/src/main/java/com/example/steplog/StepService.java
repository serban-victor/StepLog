package com.example.steplog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int totalSteps;
    private static final String TAG = "StepService";
    private static final String PREFS_NAME = "StepLogPrefs";
    private static final String KEY_TOTAL_STEPS = "total_steps";
    private static final String KEY_LAST_STEP_DATE = "last_step_date";

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "step_channel",
                    "Step Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Foreground notificare
        Notification notification = new NotificationCompat.Builder(this, "step_channel")
                .setContentTitle("StepLog")
                .setContentText("Monitorizează pașii în fundal")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        startForeground(1, notification);

        // Data curenta
        String currentDate = getCurrentDateString();

        // Initialize sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // Get stored preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String storedDate = prefs.getString(KEY_LAST_STEP_DATE, "");

        // Verifica daca data s-a schimbat
        if (!currentDate.equals(storedDate)) {
            // Reset pentru zi noua
            totalSteps = 0;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_TOTAL_STEPS, 0);
            editor.putString(KEY_LAST_STEP_DATE, currentDate);
            editor.apply();
            Log.d(TAG, "New day detected. Reset step counter to 0.");
        } else {
            // Daca e aceeasi zi citeste pasii din SharedPreferences
            totalSteps = prefs.getInt(KEY_TOTAL_STEPS, 0);
            Log.d(TAG, "Same day. Loaded existing step count: " + totalSteps);
        }

        // Sensor listener
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // Data curenta
            String currentDate = getCurrentDateString();

            // Stored preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String storedDate = prefs.getString(KEY_LAST_STEP_DATE, "");

            // Verifica daca data s-a schimbat de la ultimii pasi
            if (!currentDate.equals(storedDate)) {
                // Reseteaza masuratoarea
                totalSteps = 0;
                Log.d(TAG, "Date changed during runtime. Reset step counter to 0.");
            }

            // Step counter
            totalSteps += event.values.length;

            // Salveaza masuratoarea si data
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_TOTAL_STEPS, totalSteps);
            editor.putString(KEY_LAST_STEP_DATE, currentDate);
            editor.apply();

            Log.d(TAG, "Step detected. Total for " + currentDate + ": " + totalSteps);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     * Get current date as formatted string
     */
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
