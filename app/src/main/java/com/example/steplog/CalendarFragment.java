package com.example.steplog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedDateSteps, tvSelectedDateDistance, tvSelectedDateCalories, tvNoSessions;
    private LinearLayout llTimedSessionsContainer;
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
        llTimedSessionsContainer = view.findViewById(R.id.llTimedSessionsContainer);
        tvNoSessions = view.findViewById(R.id.tvNoSessions);

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

            Log.d("CalendarFragment", "Sessions count: " + (sessions != null ? sessions.size() : 0));

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

                    // Handle timed sessions with LinearLayout instead of RecyclerView
                    if (sessions == null || sessions.isEmpty()) {
                        tvNoSessions.setVisibility(View.VISIBLE);
                        llTimedSessionsContainer.setVisibility(View.GONE);
                    } else {
                        tvNoSessions.setVisibility(View.GONE);
                        llTimedSessionsContainer.setVisibility(View.VISIBLE);

                        // Clear previous views
                        llTimedSessionsContainer.removeAllViews();

                        // Add each session manually
                        for (TimedSessionEntry session : sessions) {
                            View sessionView = LayoutInflater.from(getContext()).inflate(R.layout.item_timed_session, llTimedSessionsContainer, false);

                            // Find views in the inflated layout
                            TextView tvSessionStartTime = sessionView.findViewById(R.id.tvItemSessionStartTime);
                            TextView tvSessionDuration = sessionView.findViewById(R.id.tvItemSessionDuration);
                            TextView tvSessionSteps = sessionView.findViewById(R.id.tvItemSessionSteps);
                            TextView tvSessionDistance = sessionView.findViewById(R.id.tvItemSessionDistance);
                            TextView tvSessionCalories = sessionView.findViewById(R.id.tvItemSessionCalories);

                            // Format and set data
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            String startTimeFormatted = timeFormat.format(new Date(session.startTime));
                            tvSessionStartTime.setText(String.format(Locale.getDefault(), "Started at: %s", startTimeFormatted));

                            // Format duration
                            long millis = session.durationMillis;
                            String durationFormatted;
                            if (TimeUnit.MILLISECONDS.toHours(millis) > 0) {
                                durationFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(millis),
                                        TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                                        TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
                            } else {
                                durationFormatted = String.format(Locale.getDefault(), "%02d:%02d",
                                        TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                                        TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
                            }
                            tvSessionDuration.setText(String.format(Locale.getDefault(), "Duration: %s", durationFormatted));

                            tvSessionSteps.setText(String.format(Locale.getDefault(), "Steps: %d", session.steps));
                            tvSessionDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", session.distanceKm));
                            tvSessionCalories.setText(String.format(Locale.getDefault(), "Calories: %.1f kcal", session.caloriesKcal));

                            // Add the view to the container
                            llTimedSessionsContainer.addView(sessionView);
                        }
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
