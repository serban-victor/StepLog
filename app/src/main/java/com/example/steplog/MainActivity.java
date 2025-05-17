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
    // private AppDatabase db; // Database instance is managed by ViewModel/AppDatabase singleton
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

        // db = AppDatabase.getInstance(getApplicationContext()); // Get instance via AppDatabase.getInstance()

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

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

        requestPermissionsIfNeeded();
        initializeSensor();

        // Load initial step count from SharedPreferences and update ViewModel
        // This ensures ViewModel is the source of truth for UI components like HomeFragment
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_date", todayDate);

        if (!todayDate.equals(savedDate)) {
            totalSteps = 0;
            // Persist the reset for the new day immediately
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_steps", 0);
            editor.putString("last_date", todayDate);
            editor.apply();
        } else {
            totalSteps = prefs.getInt("total_steps", 0);
        }
        Log.d("StepLog_Main", "Initial totalSteps loaded: " + totalSteps + " for date: " + todayDate);
        // Update the ViewModel with the steps loaded/reset from SharedPreferences.
        // HomeFragment will observe this from the ViewModel.
        sharedViewModel.setSteps(totalSteps, todayDate);

        startStepService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfNewDay(); // This updates totalSteps if it's a new day
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        // Ensure ViewModel reflects the current state, especially after checkIfNewDay might have reset totalSteps
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        sharedViewModel.setSteps(totalSteps, todayDate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            checkIfNewDay(); // Ensure totalSteps is correct for the current day
            totalSteps += event.values.length; // Increment steps
            Log.d("StepLog_Main", "Step detected. Current totalSteps: " + totalSteps);

            SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_steps", totalSteps);
            // editor.putString("last_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date())); // last_date updated in checkIfNewDay
            editor.apply();

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            sharedViewModel.setSteps(totalSteps, date); // Update ViewModel, which updates DB and LiveData for HomeFragment
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    private void initializeSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        }

        if (stepSensor == null) {
            Toast.makeText(this, "Step detector sensor not available", Toast.LENGTH_LONG).show();
        } else {
            // Toast.makeText(this, "Step detector sensor initialized", Toast.LENGTH_SHORT).show(); // Optional: can be noisy
        }
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.POST_NOTIFICATIONS // For StepService foreground notification
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION
            };
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void checkIfNewDay() {
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, MODE_PRIVATE);
        String lastSavedDate = prefs.getString("last_date", "");
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());

        if (!todayDate.equals(lastSavedDate)) {
            Log.d("StepLog_Main", "New day detected! Resetting steps. Old date: " + lastSavedDate + ", New date: " + todayDate);
            totalSteps = 0;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_steps", 0);
            editor.putString("last_date", todayDate);
            editor.apply();
            // Update ViewModel with reset steps for the new day
            sharedViewModel.setSteps(0, todayDate);
        }
    }

    private void startStepService() {
        Intent serviceIntent = new Intent(this, StepService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
                initializeSensor(); // Re-initialize or ensure sensor is active
                startStepService(); // Start service if permissions now granted
            } else {
                Toast.makeText(this, "Activity recognition permission is required for step tracking.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

