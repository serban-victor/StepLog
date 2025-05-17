package com.example.steplog;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimerFragment extends Fragment implements SensorEventListener {

    private Chronometer chronometerTimer;
    private TextView tvTimedSteps, tvTimedDistance, tvTimedCalories;
    private Button btnStartStopTimer;

    private SensorManager sensorManager;
    private Sensor stepDetectorSensor;

    private AppDatabase db;
    private SharedViewModel sharedViewModel;

    public TimerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        chronometerTimer = view.findViewById(R.id.chronometerTimer);
        tvTimedSteps = view.findViewById(R.id.tvTimedSteps);
        tvTimedDistance = view.findViewById(R.id.tvTimedDistance);
        tvTimedCalories = view.findViewById(R.id.tvTimedCalories);
        btnStartStopTimer = view.findViewById(R.id.btnStartStopTimer);

        btnStartStopTimer.setOnClickListener(v -> {
            Boolean isRunning = sharedViewModel.getIsTimerRunning().getValue();
            if (isRunning != null && isRunning) {
                stopTimedSession();
            } else {
                startTimedSession();
            }
        });

        observeViewModel();
        return view;
    }

    private void observeViewModel() {
        sharedViewModel.getIsTimerRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                btnStartStopTimer.setText("Stop");
                chronometerTimer.setBase(sharedViewModel.getChronometerBaseTime().getValue() != null ? sharedViewModel.getChronometerBaseTime().getValue() : SystemClock.elapsedRealtime());
                chronometerTimer.start();
                if (stepDetectorSensor != null) {
                    sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
                }
            } else {
                btnStartStopTimer.setText("Start");
                chronometerTimer.stop();
                // Set base to current elapsed time when stopped and not reset, so it shows the last duration
                if (sharedViewModel.getChronometerBaseTime().getValue() != null && sharedViewModel.getTimedStepCount().getValue() != null && sharedViewModel.getTimedStepCount().getValue() > 0) {
                     // If there was a session, keep its time, otherwise reset chronometer display
                } else {
                    chronometerTimer.setBase(SystemClock.elapsedRealtime());
                }
                if (sensorManager != null) {
                    sensorManager.unregisterListener(this, stepDetectorSensor);
                }
            }
            updateUI(); // Update text fields based on possibly restored step count
        });

        sharedViewModel.getTimedStepCount().observe(getViewLifecycleOwner(), steps -> {
            updateUI(); // This will recalculate distance and calories
        });

        sharedViewModel.getChronometerBaseTime().observe(getViewLifecycleOwner(), baseTime -> {
            Boolean isRunning = sharedViewModel.getIsTimerRunning().getValue();
            if (isRunning != null && isRunning && baseTime != null) {
                chronometerTimer.setBase(baseTime);
                chronometerTimer.start();
            } else {
                chronometerTimer.setBase(SystemClock.elapsedRealtime());
                chronometerTimer.stop();
            }
        });

    }

    private void startTimedSession() {
        if (stepDetectorSensor == null) {
            Toast.makeText(getContext(), "Step detector sensor not available for timed session.", Toast.LENGTH_LONG).show();
            return;
        }
        sharedViewModel.setTimerRunning(true);
        sharedViewModel.setTimedStepCount(0);
        sharedViewModel.setSessionStartTimeMillis(System.currentTimeMillis());
        sharedViewModel.setChronometerBaseTime(SystemClock.elapsedRealtime());

        Log.d("TimerFragment", "Timed session started via ViewModel.");
    }

    private void stopTimedSession() {
        sharedViewModel.setTimerRunning(false);

        long sessionStartTime = sharedViewModel.getSessionStartTimeMillis().getValue() != null ? sharedViewModel.getSessionStartTimeMillis().getValue() : 0;
        long sessionEndTimeMillis = System.currentTimeMillis();
        long durationMillis = (sessionStartTime > 0) ? (sessionEndTimeMillis - sessionStartTime) : 0;
        
        // If chronometer was running, its base is the start, current elapsedRealtime - base is duration
        Long base = sharedViewModel.getChronometerBaseTime().getValue();
        if (base != null && base < SystemClock.elapsedRealtime()) {
            durationMillis = SystemClock.elapsedRealtime() - base;
        }

        final long finalDurationMillis = durationMillis;
        final int finalTimedStepCount = sharedViewModel.getTimedStepCount().getValue() != null ? sharedViewModel.getTimedStepCount().getValue() : 0;

        if (finalTimedStepCount == 0 && finalDurationMillis < 1000) { // Avoid saving empty/too short sessions
            Log.d("TimerFragment", "Timed session too short or no steps, not saving.");
            sharedViewModel.resetTimerState(); // Reset UI and state in ViewModel
            Toast.makeText(getContext(), "Session too short, not saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        float userHeightCm = 0;
        float userWeightKg = 0;
        if (getContext() != null) {
            userHeightCm = getContext().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE).getFloat(UserFragment.KEY_HEIGHT, 0);
            userWeightKg = getContext().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE).getFloat(UserFragment.KEY_WEIGHT, 0);
        }

        double distanceKm = SharedViewModel.calculateDistanceKm(finalTimedStepCount, userHeightCm);
        double caloriesKcal = SharedViewModel.calculateCaloriesKcal(finalTimedStepCount, userWeightKg);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(sessionStartTime > 0 ? sessionStartTime : sessionEndTimeMillis));

        TimedSessionEntry sessionEntry = new TimedSessionEntry(
                currentDate,
                sessionStartTime > 0 ? sessionStartTime : (sessionEndTimeMillis - finalDurationMillis), // Estimate start if not set
                sessionEndTimeMillis,
                finalDurationMillis,
                finalTimedStepCount,
                distanceKm,
                caloriesKcal
        );

        new Thread(() -> {
            db.timedSessionEntryDao().insert(sessionEntry);
            Log.d("TimerFragment", "Timed session saved: Steps - " + finalTimedStepCount + ", Duration - " + finalDurationMillis + "ms");
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Timed session saved!", Toast.LENGTH_SHORT).show();
                    sharedViewModel.resetTimerState(); // Reset UI and state in ViewModel
                });
            }
        }).start();
    }

    private void updateUI() {
        Integer steps = sharedViewModel.getTimedStepCount().getValue();
        if (steps == null) steps = 0;
        tvTimedSteps.setText(String.format(Locale.getDefault(), "Steps: %d", steps));

        float userHeightCm = 0;
        float userWeightKg = 0;
        String unitSystem = "metric";
        if (getContext() != null) {
            userHeightCm = getContext().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE).getFloat(UserFragment.KEY_HEIGHT, 0);
            userWeightKg = getContext().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE).getFloat(UserFragment.KEY_WEIGHT, 0);
            unitSystem = getContext().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE).getString(SettingsFragment.KEY_UNITS, "metric");
        }

        double distanceKm = SharedViewModel.calculateDistanceKm(steps, userHeightCm);
        double caloriesKcal = SharedViewModel.calculateCaloriesKcal(steps, userWeightKg);

        if ("imperial".equals(unitSystem)) {
            double distanceMiles = distanceKm * SharedViewModel.MILES_PER_KM;
            tvTimedDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f miles", distanceMiles));
        } else {
            tvTimedDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm));
        }
        tvTimedCalories.setText(String.format(Locale.getDefault(), "Calories: %.1f kcal", caloriesKcal));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Boolean isRunning = sharedViewModel.getIsTimerRunning().getValue();
        if (isRunning != null && isRunning && event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            sharedViewModel.incrementTimedStepCount((int) event.values[0]); // event.values[0] is usually 1.0f for step detector
            Log.d("TimerFragment", "Timed step detected. ViewModel steps: " + sharedViewModel.getTimedStepCount().getValue());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-observe or re-apply state if necessary, though LiveData should handle most of it.
        // If sensor needs to be re-registered based on ViewModel state when fragment resumes:
        Boolean isRunning = sharedViewModel.getIsTimerRunning().getValue();
        if (isRunning != null && isRunning && stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
        updateUI(); // Ensure UI is current on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister sensor if it was registered by this fragment instance directly
        // ViewModel driven registration in observeViewModel should handle this mostly.
        // However, good practice to unregister here if not tied to lifecycle owner observation for sensor.
        Boolean isRunning = sharedViewModel.getIsTimerRunning().getValue();
        if (isRunning != null && isRunning) {
             // sensorManager.unregisterListener(this, stepDetectorSensor); // Consider if this is needed or if observe handles it
        } // If timer is running, we want the service (if any) or MainActivity to keep counting, not this fragment if it's paused.
          // The current implementation relies on this fragment being active for step counting in timed session.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Sensor should be unregistered by LiveData observer when view is destroyed
        // or explicitly in onPause if not strictly tied to view lifecycle for registration.
        // If timer is running, we might want to log or handle this case.
        Boolean isRunning = sharedViewModel.getIsTimerRunning().getValue();
        if (isRunning != null && isRunning) {
            Log.w("TimerFragment", "View destroyed while timer was running in ViewModel. Session state persists in ViewModel.");
        }
    }
}

