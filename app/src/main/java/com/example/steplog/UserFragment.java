package com.example.steplog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

public class UserFragment extends Fragment {

    private TextInputEditText etHeight, etWeight, etAge;
    private RadioGroup rgGender;
    private Button btnSaveUserData;

    public static final String PREFS_NAME = "StepLogPrefs";
    public static final String KEY_HEIGHT = "user_height";
    public static final String KEY_WEIGHT = "user_weight";
    public static final String KEY_AGE = "user_age";
    public static final String KEY_GENDER = "user_gender";

    public UserFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        etHeight = view.findViewById(R.id.etHeight);
        etWeight = view.findViewById(R.id.etWeight);
        etAge = view.findViewById(R.id.etAge);
        rgGender = view.findViewById(R.id.rgGender);
        btnSaveUserData = view.findViewById(R.id.btnSaveUserData);

        loadUserData();

        btnSaveUserData.setOnClickListener(v -> saveUserData());

        return view;
    }

    private void loadUserData() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        float height = prefs.getFloat(KEY_HEIGHT, 0);
        if (height > 0) etHeight.setText(String.valueOf(height));

        float weight = prefs.getFloat(KEY_WEIGHT, 0);
        if (weight > 0) etWeight.setText(String.valueOf(weight));

        int age = prefs.getInt(KEY_AGE, 0);
        if (age > 0) etAge.setText(String.valueOf(age));

        String gender = prefs.getString(KEY_GENDER, "Prefer not to say");
        switch (gender) {
            case "Male":
                rgGender.check(R.id.rbMale);
                break;
            case "Female":
                rgGender.check(R.id.rbFemale);
                break;
            default:
                rgGender.check(R.id.rbPreferNotToSay);
                break;
        }
    }

    private void saveUserData() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();


        if (!TextUtils.isEmpty(etHeight.getText())) {
            try {
                float height = Float.parseFloat(etHeight.getText().toString());
                if (height > 0) {
                    editor.putFloat(KEY_HEIGHT, height);
                } else {
                    editor.remove(KEY_HEIGHT);
                }
            } catch (NumberFormatException e) {
                etHeight.setError("Invalid height");
                return;
            }
        } else {
            editor.remove(KEY_HEIGHT);
        }


        if (!TextUtils.isEmpty(etWeight.getText())) {
            try {
                float weight = Float.parseFloat(etWeight.getText().toString());
                if (weight > 0) {
                    editor.putFloat(KEY_WEIGHT, weight);
                } else {
                    editor.remove(KEY_WEIGHT);
                }
            } catch (NumberFormatException e) {
                etWeight.setError("Invalid weight");
                return;
            }
        } else {
            editor.remove(KEY_WEIGHT);
        }


        if (!TextUtils.isEmpty(etAge.getText())) {
            try {
                int age = Integer.parseInt(etAge.getText().toString());
                if (age > 0) {
                    editor.putInt(KEY_AGE, age);
                } else {
                    editor.remove(KEY_AGE);
                }
            } catch (NumberFormatException e) {
                etAge.setError("Invalid age");
                return;
            }
        } else {
            editor.remove(KEY_AGE);
        }


        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String genderValue = "Prefer not to say";
        if (selectedGenderId == R.id.rbMale) {
            genderValue = "Male";
        } else if (selectedGenderId == R.id.rbFemale) {
            genderValue = "Female";
        }
        editor.putString(KEY_GENDER, genderValue);

        editor.apply();
        Toast.makeText(getContext(), "User data saved!", Toast.LENGTH_SHORT).show();
    }
}

