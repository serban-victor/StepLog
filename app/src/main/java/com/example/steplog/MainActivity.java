package com.example.steplog;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private int totalSteps = 0;
    BottomNavigationView bottomNavigationView;
    Fragment homeFragment = new HomeFragment();
    Fragment timerFragment = new TimerFragment();
    Fragment userFragment = new UserFragment();
    Fragment calendarFragment = new CalendarFragment();
    Fragment settingsFragment = new SettingsFragment();

    private SensorManager sensorManager;
    private Sensor stepSensor;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_nav_menu);
        setCurrentFragment(homeFragment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                setCurrentFragment(homeFragment);
            } else if (itemId == R.id.nav_timer) {
                setCurrentFragment(timerFragment);
            } else if (itemId == R.id.nav_user) {
                setCurrentFragment(userFragment);
            } else if (itemId == R.id.nav_calendar) {
                setCurrentFragment(calendarFragment);
            } else if (itemId == R.id.nav_settings) {
                setCurrentFragment(settingsFragment);
            }
            return true;
        });

        // Cerere permisiuni necesare
        requestPermissionsIfNeeded();

        // Inițializare senzori
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (stepSensor == null) {
            Toast.makeText(this, "Step detector sensor is not present on this device", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Step detector sensor found", Toast.LENGTH_LONG).show();
        }

        // Încarcă pașii din SharedPreferences
        SharedPreferences prefs = getSharedPreferences("StepLogPrefs", MODE_PRIVATE);
        totalSteps = prefs.getInt("total_steps", 0);
        Log.d("StepLog", "Pași încărcați: " + totalSteps);

        // Trimite date către HomeFragment
        if (homeFragment instanceof HomeFragment) {
            ((HomeFragment) homeFragment).updateStepData(totalSteps);
        }

        // Pornește serviciul foreground pentru pași
        startStepService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            totalSteps += event.values.length;

            Log.d("StepLog", "Pas detectat. Total: " + totalSteps);

            SharedPreferences prefs = getSharedPreferences("StepLogPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_steps", totalSteps);
            editor.apply();

            if (homeFragment instanceof HomeFragment) {
                ((HomeFragment) homeFragment).updateStepData(totalSteps);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Neutilizat
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    private void startStepService() {
        Intent serviceIntent = new Intent(this, StepService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION
            };
        }

        boolean needRequest = false;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    // Rezultatul permisiunilor
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permisiuni refuzate! Pașii pot să nu fie înregistrați corect.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Log.d("StepLog", "Toate permisiunile au fost acordate.");
        }
    }
}
