package com.example.steplog;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class HomeFragment extends Fragment {
    private TextView tvSteps, tvDistance, tvCalories;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvSteps = view.findViewById(R.id.tvSteps);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvCalories = view.findViewById(R.id.tvCalories);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            int savedSteps = getActivity()
                    .getSharedPreferences("StepLogPrefs", getActivity().MODE_PRIVATE)
                    .getInt("total_steps", 0);

            updateStepData(savedSteps);
        }
    }


    public void updateStepData(int steps) {
        if (getActivity() == null) return;

        double distance = steps * 0.78; // 78cm pas mediu
        double calories = steps * 0.04; // 0.04 kcal / pas estimat
        Log.d("StepLog", "Pași actualizați: " + steps);
        tvSteps.setText("Steps: " + steps);
        tvDistance.setText(String.format("Distance: %.2f m", distance));
        tvCalories.setText(String.format("Calories: %.2f kcal", calories));
    }
}