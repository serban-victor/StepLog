package com.example.steplog;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HomeFragment extends Fragment {
    private SharedViewModel sharedViewModel;

    private TextView tvSteps, tvDistance, tvCalories;
    private ProgressBar progressBarSteps;
    private static final int STEP_GOAL = 6000; // obiectivul de 6000 de pași

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvSteps = view.findViewById(R.id.tvSteps);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvCalories = view.findViewById(R.id.tvCalories);
        progressBarSteps = view.findViewById(R.id.progressBarSteps);
        progressBarSteps.setMax(STEP_GOAL);

        // Inițializăm SharedViewModel dacă este necesar
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            // Încărcăm pașii salvați din SharedPreferences
            int savedSteps = getActivity()
                    .getSharedPreferences("StepLogPrefs", getActivity().MODE_PRIVATE)
                    .getInt("total_steps", 0);

            // Actualizăm datele pașilor
            updateStepData(savedSteps);
        }
    }

    public void updateStepData(int steps) {
        if (getActivity() == null) return;

        // Calculăm distanța și caloriile pe baza pașilor
        double distanceInMeters = steps * 0.78; // pas mediu
        double distanceInKm = distanceInMeters / 1000.0; // transformare în kilometri
        double calories = steps * 0.04; // 0.04 kcal / pas

        // Log pentru debugging
        Log.d("StepLog", "Pași actualizați: " + steps);

        // Actualizăm textul pe UI
        tvSteps.setText("Steps: " + steps);
        tvDistance.setText(String.format("Distance: %.2f km", distanceInKm));
        tvCalories.setText(String.format("Calories: %.2f kcal", calories));

        // Actualizăm progress bar-ul cu pașii actuali
        progressBarSteps.setProgress(Math.min(steps, STEP_GOAL));  // Se asigură că nu depășim obiectivul
    }

}
