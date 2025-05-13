package com.example.steplog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private static final String PREF_NAME = "AppSettings";
    private static final String PREF_THEME_KEY = "AppTheme";
    private static final String PREF_NOTIFICATIONS_KEY = "NotificationsEnabled";
    private static final String PREF_UNIT_SYSTEM = "UnitSystem";
    private static final String PREF_STEP_GOAL = "StepGoal";

    private RadioGroup themeRadioGroup;
    private RadioButton lightModeRadioButton, darkModeRadioButton, systemModeRadioButton;
    private Switch unitSystemSwitch, notificationsSwitch;
    private EditText stepGoalEditText;
    private Button resetButton;

    private SharedPreferences sharedPreferences;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        themeRadioGroup = view.findViewById(R.id.theme_radio_group);
        lightModeRadioButton = view.findViewById(R.id.light_mode_radio_button);
        darkModeRadioButton = view.findViewById(R.id.dark_mode_radio_button);
        systemModeRadioButton = view.findViewById(R.id.system_mode_radio_button);
        unitSystemSwitch = view.findViewById(R.id.unit_system_switch);
        notificationsSwitch = view.findViewById(R.id.notifications_switch);
        stepGoalEditText = view.findViewById(R.id.step_goal_edit_text);
        resetButton = view.findViewById(R.id.button_reset);

        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadSettings();
        setupListeners();
    }

    private void loadSettings() {
        int savedTheme = sharedPreferences.getInt(PREF_THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switch (savedTheme) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                lightModeRadioButton.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                darkModeRadioButton.setChecked(true);
                break;
            default:
                systemModeRadioButton.setChecked(true);
                break;
        }
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        unitSystemSwitch.setChecked(sharedPreferences.getBoolean(PREF_UNIT_SYSTEM, true));
        notificationsSwitch.setChecked(sharedPreferences.getBoolean(PREF_NOTIFICATIONS_KEY, true));
        int stepGoal = sharedPreferences.getInt(PREF_STEP_GOAL, 10000);
        stepGoalEditText.setText(String.valueOf(stepGoal));
    }

    private void saveThemeSetting(int themeMode) {
        sharedPreferences.edit().putInt(PREF_THEME_KEY, themeMode).apply();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void saveUnitSystemSetting(boolean isMetric) {
        sharedPreferences.edit().putBoolean(PREF_UNIT_SYSTEM, isMetric).apply();
    }

    private void saveNotificationsSetting(boolean enabled) {
        sharedPreferences.edit().putBoolean(PREF_NOTIFICATIONS_KEY, enabled).apply();
    }

    private void saveStepGoal(int stepGoal) {
        sharedPreferences.edit().putInt(PREF_STEP_GOAL, stepGoal).apply();
    }

    private void setupListeners() {
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.light_mode_radio_button) {
                saveThemeSetting(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.dark_mode_radio_button) {
                saveThemeSetting(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                saveThemeSetting(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });

        unitSystemSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveUnitSystemSetting(isChecked));
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveNotificationsSetting(isChecked));

        stepGoalEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String stepGoalStr = stepGoalEditText.getText().toString();
                try {
                    int stepGoal = Integer.parseInt(stepGoalStr);
                    if (stepGoal > 0) {
                        saveStepGoal(stepGoal);
                    } else {
                        showInvalidStepGoal();
                    }
                } catch (NumberFormatException e) {
                    showInvalidStepGoal();
                }
            }
        });

        resetButton.setOnClickListener(v -> {
            sharedPreferences.edit()
                    .putInt(PREF_THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    .putBoolean(PREF_NOTIFICATIONS_KEY, true)
                    .putBoolean(PREF_UNIT_SYSTEM, true)
                    .putInt(PREF_STEP_GOAL, 10000)
                    .apply();
            loadSettings();
            Toast.makeText(getContext(), "All settings reset to default.", Toast.LENGTH_SHORT).show();
        });
    }

    private void showInvalidStepGoal() {
        Toast.makeText(getContext(), "Invalid step goal. Please enter a positive number.", Toast.LENGTH_SHORT).show();
        int validStepGoal = sharedPreferences.getInt(PREF_STEP_GOAL, 10000);
        stepGoalEditText.setText(String.valueOf(validStepGoal));
    }
}
