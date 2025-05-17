package com.example.steplog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private RadioGroup rgTheme, rgUnits;
    private SwitchMaterial switchNotifications;
    private TextInputEditText etStepGoal;
    private Button btnSaveSettings, btnResetAllData;

    public static final String PREFS_NAME = "StepLogPrefs";
    public static final String KEY_THEME = "theme_preference";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_UNITS = "measurement_units"; // "metric" or "imperial"
    public static final String KEY_STEP_GOAL = "step_goal";

    private AppDatabase db;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        rgTheme = view.findViewById(R.id.rgTheme);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        rgUnits = view.findViewById(R.id.rgUnits);
        etStepGoal = view.findViewById(R.id.etStepGoal);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);
        btnResetAllData = view.findViewById(R.id.btnResetAllData);

        loadSettings();

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> saveThemePreference(checkedId));
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> saveNotificationsPreference(isChecked));
        rgUnits.setOnCheckedChangeListener((group, checkedId) -> saveUnitsPreference(checkedId));
        btnSaveSettings.setOnClickListener(v -> saveStepGoal());
        btnResetAllData.setOnClickListener(v -> confirmResetData());

        return view;
    }

    private void loadSettings() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Theme
        int themeMode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            rgTheme.check(R.id.rbThemeLight);
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            rgTheme.check(R.id.rbThemeDark);
        } else {
            rgTheme.check(R.id.rbThemeSystem);
        }

        // Notifications
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        switchNotifications.setChecked(notificationsEnabled);

        // Units
        String units = prefs.getString(KEY_UNITS, "metric");
        if ("imperial".equals(units)) {
            rgUnits.check(R.id.rbUnitsImperial);
        } else {
            rgUnits.check(R.id.rbUnitsMetric);
        }

        // Step Goal
        int stepGoal = prefs.getInt(KEY_STEP_GOAL, 6000);
        etStepGoal.setText(String.valueOf(stepGoal));
    }

    private void saveThemePreference(int checkedId) {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

        if (checkedId == R.id.rbThemeLight) {
            themeMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (checkedId == R.id.rbThemeDark) {
            themeMode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        // For R.id.rbThemeSystem, it remains MODE_NIGHT_FOLLOW_SYSTEM

        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void saveNotificationsPreference(boolean isChecked) {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked);
        editor.apply();
        // Logic to enable/disable actual notifications would be in StepService or MainActivity
        Toast.makeText(getContext(), "Notification preference saved.", Toast.LENGTH_SHORT).show();
    }

    private void saveUnitsPreference(int checkedId) {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String units = "metric";
        if (checkedId == R.id.rbUnitsImperial) {
            units = "imperial";
        }
        editor.putString(KEY_UNITS, units);
        editor.apply();
        Toast.makeText(getContext(), "Units preference saved. App may need restart for full effect on calculations.", Toast.LENGTH_LONG).show();
        // Other fragments need to read this preference and adjust their display/calculations.
    }

    private void saveStepGoal() {
        if (getContext() == null || TextUtils.isEmpty(etStepGoal.getText())) return;
        try {
            int stepGoal = Integer.parseInt(etStepGoal.getText().toString());
            if (stepGoal <= 0) {
                etStepGoal.setError("Goal must be positive");
                return;
            }
            SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_STEP_GOAL, stepGoal);
            editor.apply();
            Toast.makeText(getContext(), "Step goal saved!", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            etStepGoal.setError("Invalid number");
        }
    }

    private void confirmResetData() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Reset All Data")
                .setMessage("Are you sure you want to delete all your step data, timed sessions, and user profile information? This action cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> resetAllData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetAllData() {
        if (getContext() == null) return;
        // Clear SharedPreferences
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Keep theme, but clear others
        editor.remove(UserFragment.KEY_HEIGHT);
        editor.remove(UserFragment.KEY_WEIGHT);
        editor.remove(UserFragment.KEY_AGE);
        editor.remove(UserFragment.KEY_GENDER);
        editor.remove(KEY_NOTIFICATIONS_ENABLED); // Or set to default
        editor.remove(KEY_UNITS); // Or set to default
        editor.remove(KEY_STEP_GOAL); // Or set to default
        editor.remove("total_steps"); // From MainActivity/StepService
        editor.remove("last_date");   // From MainActivity/StepService
        editor.apply();

        // Clear Database
        new Thread(() -> {
            db.stepEntryDao().deleteAllEntries();
            db.timedSessionEntryDao().deleteAllSessions();
            Log.d("SettingsFragment", "All database entries deleted.");

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "All data has been reset.", Toast.LENGTH_LONG).show();
                    // Reload settings to reflect defaults
                    loadSettings();
                    // Potentially navigate user or refresh other parts of the app
                });
            }
        }).start();
    }
}

