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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // Shared preferences keys
    private static final String PREF_NAME = "AppSettings";
    private static final String PREF_THEME_KEY = "AppTheme";
    private static final String PREF_NOTIFICATIONS_KEY = "NotificationsEnabled";
    private static final String PREF_UNIT_SYSTEM = "UnitSystem";
    private static final String PREF_STEP_GOAL = "StepGoal";

    // UI elements
    private RadioGroup themeRadioGroup;
    private RadioButton lightModeRadioButton;
    private RadioButton darkModeRadioButton;
    private RadioButton systemModeRadioButton;
    private Switch unitSystemSwitch;
    private Switch notificationsSwitch;
    private EditText stepGoalEditText;
    private Button resetButton;

    private SharedPreferences sharedPreferences;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize UI elements
        themeRadioGroup = view.findViewById(R.id.theme_radio_group);
        lightModeRadioButton = view.findViewById(R.id.light_mode_radio_button);
        darkModeRadioButton = view.findViewById(R.id.dark_mode_radio_button);
        systemModeRadioButton = view.findViewById(R.id.system_mode_radio_button);
        unitSystemSwitch = view.findViewById(R.id.unit_system_switch);
        notificationsSwitch = view.findViewById(R.id.notifications_switch);
        stepGoalEditText = view.findViewById(R.id.step_goal_edit_text);
        resetButton = view.findViewById(R.id.button_reset);

        // Initialize shared preferences
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load saved settings
        loadSettings();

        // Set up listeners for settings changes
        setupListeners();
    }

    private void loadSettings() {
        // Load theme setting
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

        // Load unit system setting
        boolean isMetric = sharedPreferences.getBoolean(PREF_UNIT_SYSTEM, true);
        unitSystemSwitch.setChecked(isMetric);

        // Load notifications setting
        boolean notificationsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATIONS_KEY, true);
        notificationsSwitch.setChecked(notificationsEnabled);

        //Load step goal
        int stepGoal = sharedPreferences.getInt(PREF_STEP_GOAL, 10000);
        stepGoalEditText.setText(String.valueOf(stepGoal));
    }

    private void saveThemeSetting(int themeMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_THEME_KEY, themeMode);
        editor.apply();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void saveUnitSystemSetting(boolean isMetric) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_UNIT_SYSTEM, isMetric);
        editor.apply();
    }

    private void saveNotificationsSetting(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_NOTIFICATIONS_KEY, enabled);
        editor.apply();
    }

    private void saveStepGoal(int stepGoal) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_STEP_GOAL, stepGoal);
        editor.apply();
    }

    private void setupListeners() {
        themeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.light_mode_radio_button) {
                    saveThemeSetting(AppCompatDelegate.MODE_NIGHT_NO);
                } else if (checkedId == R.id.dark_mode_radio_button) {
                    saveThemeSetting(AppCompatDelegate.MODE_NIGHT_YES);
                } else if (checkedId == R.id.system_mode_radio_button) {
                    saveThemeSetting(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
            }
        });

        unitSystemSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveUnitSystemSetting(isChecked);
            }
        });

        notificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveNotificationsSetting(isChecked);
            }
        });

        stepGoalEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String stepGoalStr = stepGoalEditText.getText().toString();
                    try {
                        int stepGoal = Integer.parseInt(stepGoalStr);
                        if (stepGoal > 0) {
                            saveStepGoal(stepGoal);
                        } else {
                            Toast.makeText(getContext(), "Invalid step goal. Please enter a positive number.", Toast.LENGTH_SHORT).show();
                            //Reload the last valid value
                            int validStepGoal = sharedPreferences.getInt(PREF_STEP_GOAL, 10000);
                            stepGoalEditText.setText(String.valueOf(validStepGoal));
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid step goal format.", Toast.LENGTH_SHORT).show();
                        //Reload the last valid value
                        int validStepGoal = sharedPreferences.getInt(PREF_STEP_GOAL, 10000);
                        stepGoalEditText.setText(String.valueOf(validStepGoal));
                    }
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset all settings to default values
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREF_THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                editor.putBoolean(PREF_NOTIFICATIONS_KEY, true);
                editor.putBoolean(PREF_UNIT_SYSTEM, true);
                editor.putInt(PREF_STEP_GOAL, 10000);
                editor.apply();

                // Reload the settings
                loadSettings();

                Toast.makeText(getContext(), "All settings reset to default.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
