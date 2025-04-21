package com.example.steplog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verifică permisiunea pentru Activity Recognition
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION},
                    1);
        }

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

        // Inițializarea senzorilor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            Toast.makeText(this, "Step counter sensor is not present on this device", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Step counter sensor found", Toast.LENGTH_LONG).show();
        }

        // Încarcă pașii salvați din SharedPreferences
        SharedPreferences prefs = getSharedPreferences("StepLogPrefs", MODE_PRIVATE);
        totalSteps = prefs.getInt("total_steps", 0); // 0 este valoarea implicită
        Log.d("StepLog", "Pași încărcați: " + totalSteps);

        // Răspunde la pași în HomeFragment
        if (homeFragment instanceof HomeFragment) {
            ((HomeFragment) homeFragment).updateStepData(totalSteps);
        }
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
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];
            Log.d("StepLog", "Pași detectați: " + totalSteps);

            // Salvăm pașii în SharedPreferences
            SharedPreferences prefs = getSharedPreferences("StepLogPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_steps", totalSteps);
            editor.apply();

            // Trimitem pașii către HomeFragment
            if (homeFragment instanceof HomeFragment) {
                ((HomeFragment) homeFragment).updateStepData(totalSteps);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // optional
    }

    // Metoda pentru gestionarea permisiunii
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisiune acordată, poți activa senzorul
                Log.d("StepLog", "Permisiunea a fost acordată.");
            } else {
                // Permisiune refuzată, afisează un mesaj
                Log.d("StepLog", "Permisiunea a fost refuzată.");
            }
        }
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}
