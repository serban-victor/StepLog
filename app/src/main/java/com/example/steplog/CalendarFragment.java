package com.example.steplog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedDateSteps, tvSelectedDateDistance, tvSelectedDateCalories, tvNoSessions;
    private RecyclerView rvTimedSessions;
    private TimedSessionAdapter timedSessionAdapter;
    private List<TimedSessionEntry> timedSessionsList = new ArrayList<>();
    private AppDatabase db;
    private String selectedDateString; // To store the currently selected date as YYYY-MM-DD

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedDateSteps = view.findViewById(R.id.tvSelectedDateSteps);
        tvSelectedDateDistance = view.findViewById(R.id.tvSelectedDateDistance);
        tvSelectedDateCalories = view.findViewById(R.id.tvSelectedDateCalories);
        rvTimedSessions = view.findViewById(R.id.rvTimedSessions);
        tvNoSessions = view.findViewById(R.id.tvNoSessions);

        rvTimedSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        timedSessionAdapter = new TimedSessionAdapter(timedSessionsList);
        rvTimedSessions.setAdapter(timedSessionAdapter);

        // Initialize with today's date
        selectedDateString = getFormattedDate(calendarView.getDate());
        loadDataForSelectedDate(selectedDateString);

        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            selectedDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Log.d("CalendarFragment", "Date selected: " + selectedDateString);
            loadDataForSelectedDate(selectedDateString);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data if a date was previously selected
        if (selectedDateString != null) {
            loadDataForSelectedDate(selectedDateString);
        }
    }

    private void loadDataForSelectedDate(String date) {
        new Thread(() -> {
            // Load daily step entry
            StepEntry dailyEntry = db.stepEntryDao().getEntryByDate(date);

            // Load timed sessions for the date
            List<TimedSessionEntry> sessions = db.timedSessionEntryDao().getSessionsByDate(date);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (dailyEntry != null) {
                        tvSelectedDateSteps.setText(String.format(Locale.getDefault(), "Steps: %d", dailyEntry.steps));
                        tvSelectedDateDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", dailyEntry.distance));
                        tvSelectedDateCalories.setText(String.format(Locale.getDefault(), "Calories: %.1f kcal", dailyEntry.calories));
                    } else {
                        tvSelectedDateSteps.setText("Steps: --");
                        tvSelectedDateDistance.setText("Distance: -- km");
                        tvSelectedDateCalories.setText("Calories: -- kcal");
                    }

                    timedSessionAdapter.updateSessions(sessions);
                    if (sessions == null || sessions.isEmpty()) {
                        tvNoSessions.setVisibility(View.VISIBLE);
                        rvTimedSessions.setVisibility(View.GONE);
                    } else {
                        tvNoSessions.setVisibility(View.GONE);
                        rvTimedSessions.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private String getFormattedDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}

