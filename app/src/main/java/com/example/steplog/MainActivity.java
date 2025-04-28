package com.example.steplog;

import android.Manifest;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int totalSteps = 0;
    private AppDatabase db;
    private SharedViewModel sharedViewModel;

    private BottomNavigationView bottomNavigationView;
    private Fragment homeFragment = new HomeFragment();
    private Fragment timerFragment = new TimerFragment();
    private Fragment userFragment = new UserFragment();
    private Fragment calendarFragment = new CalendarFragment();
    private Fragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = androidx.room.Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "step_log_db"
        ).allowMainThreadQueries().build();

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        bottomNavigationView = findViewById(R.id.bottom_nav_menu);

        // Schimbă fragmentul inițial pe care îl dorim (homeFragment)
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

        requestPermissionsIfNeeded();
        initializeSensor();

        SharedPreferences prefs = getSharedPreferences("StepLogPrefs", MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_date", todayDate);

        if (!todayDate.equals(savedDate)) {
            totalSteps = 0;
        } else {
            totalSteps = prefs.getInt("total_steps", 0);
        }
        
        Log.d("StepLog", "Pași încărcați: " + totalSteps);

        // Actualizează HomeFragment cu pașii încărcați
        if (homeFragment instanceof HomeFragment) {
            ((HomeFragment) homeFragment).updateStepData(totalSteps);
        }

        startStepService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfNewDay();
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
            checkIfNewDay();
            totalSteps += event.values.length;
            Log.d("StepLog", "Pas detectat. Total: " + totalSteps);

            // Salvează totalul pașilor în SharedPreferences
            SharedPreferences prefs = getSharedPreferences("StepLogPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_steps", totalSteps);
            editor.apply();

            // Actualizează pașii în ViewModel și baza de date
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            sharedViewModel.setSteps(totalSteps, date);

            // Actualizează HomeFragment
            if (homeFragment instanceof HomeFragment) {
                ((HomeFragment) homeFragment).updateStepData(totalSteps);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nu avem nevoie acum
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    private void initializeSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (stepSensor == null) {
            Toast.makeText(this, "Step detector sensor not available", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Step detector sensor initialized", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.FOREGROUND_SERVICE
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION
            };
        }

        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void checkIfNewDay() {
        SharedPreferences prefs = getSharedPreferences("StepLogPrefs", MODE_PRIVATE);
        String lastSavedDate = prefs.getString("last_date", "");
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());

        if (!todayDate.equals(lastSavedDate)) {
            // Nouă zi detectată, resetăm pașii
            totalSteps = 0;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_steps", 0);
            editor.putString("last_date", todayDate);
            editor.apply();
            Log.d("StepLog", "Nouă zi detectată! Pașii au fost resetați.");
        }
    }

    private void startStepService() {
        Intent serviceIntent = new Intent(this, StepService.class);
        startService(serviceIntent);
    }

}
