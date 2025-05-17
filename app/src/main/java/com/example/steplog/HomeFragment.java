package com.example.steplog;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private SharedViewModel sharedViewModel;
    private SharedPreferences prefs;

    private TextView tvSteps, tvDistance, tvCalories;
    private ProgressBar progressBarSteps;
    private BarChart weekBarChart;
    private int currentStepGoal = 6000; // Default step goal
    private String currentUnitSystem = "metric";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            prefs = getContext().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvSteps = view.findViewById(R.id.tvSteps);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvCalories = view.findViewById(R.id.tvCalories);
        progressBarSteps = view.findViewById(R.id.progressBarSteps);
        weekBarChart = view.findViewById(R.id.weekBarChart);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Observe LiveData for the full current day StepEntry
        sharedViewModel.getCurrentDayEntry().observe(getViewLifecycleOwner(), entry -> {
            if (entry != null) {
                updateStepDataDisplay(entry);
            }
        });

        // Observe LiveData for weekly step data
        sharedViewModel.getWeeklySteps().observe(getViewLifecycleOwner(), weeklyEntries -> {
            if (weeklyEntries != null) {
                updateWeeklyGraph(weeklyEntries);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSettings();
        progressBarSteps.setMax(currentStepGoal);

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        sharedViewModel.loadSteps(todayDate); // This will trigger currentDayEntryLiveData update
        sharedViewModel.loadWeeklySteps();
    }

    private void loadSettings() {
        if (prefs != null) {
            currentStepGoal = prefs.getInt(SettingsFragment.KEY_STEP_GOAL, 6000);
            currentUnitSystem = prefs.getString(SettingsFragment.KEY_UNITS, "metric");
        }
    }

    public void updateStepDataDisplay(StepEntry entry) {
        if (getActivity() == null || entry == null) return;

        tvSteps.setText(String.format(Locale.getDefault(), "Steps: %d", entry.steps));

        double distanceToDisplay = entry.distance; // This is in KM from ViewModel
        String distanceUnit = "km";
        if ("imperial".equals(currentUnitSystem)) {
            distanceToDisplay = entry.distance * SharedViewModel.MILES_PER_KM;
            distanceUnit = "miles";
        }
        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f %s", distanceToDisplay, distanceUnit));

        // Calories are unit-agnostic (kcal)
        tvCalories.setText(String.format(Locale.getDefault(), "Calories: %.1f kcal", entry.calories));

        progressBarSteps.setProgress(Math.min(entry.steps, currentStepGoal));
    }

    private void updateWeeklyGraph(List<StepEntry> weeklyEntries) {
        if (weeklyEntries == null || weeklyEntries.isEmpty()) {
            if(weekBarChart != null) {
                weekBarChart.clear();
                weekBarChart.invalidate();
            }
            return;
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> dayLabels = new ArrayList<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            dayLabels.add(dayFormat.format(cal.getTime()));

            StepEntry entryForDay = null;
            for (StepEntry dbEntry : weeklyEntries) {
                if (dbEntry.date.equals(dateString)) {
                    entryForDay = dbEntry;
                    break;
                }
            }
            if (entryForDay != null) {
                barEntries.add(new BarEntry(6 - i, entryForDay.steps));
            } else {
                barEntries.add(new BarEntry(6 - i, 0));
            }
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Daily Steps This Week");
        barDataSet.setColor(Color.rgb(60, 179, 113));
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(10f);

        BarData barData = new BarData(barDataSet);
        if (weekBarChart != null) {
            weekBarChart.setData(barData);

            XAxis xAxis = weekBarChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setDrawGridLines(false);

            weekBarChart.getDescription().setEnabled(false);
            weekBarChart.animateY(1000);
            weekBarChart.setFitBars(true);
            weekBarChart.invalidate();
        }
    }
}

