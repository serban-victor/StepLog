package com.example.steplog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView textSelectedDateInfo;
    private AppDatabase db;
    private String selectedDate; // memorăm data selectată

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        textSelectedDateInfo = view.findViewById(R.id.textSelectedDateInfo);
        db = AppDatabase.getInstance(requireContext());

        selectedDate = getFormattedDate(calendarView.getDate());
        loadStepData(selectedDate);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadStepData(selectedDate);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // De fiecare dată când revenim în fragment, reîncărcăm datele
        if (selectedDate == null) {
            selectedDate = getFormattedDate(calendarView.getDate());
        }
        loadStepData(selectedDate);
    }

    private void loadStepData(String date) {
        new Thread(() -> {
            StepEntry entry = db.stepEntryDao().getEntryByDate(date);

            requireActivity().runOnUiThread(() -> {
                if (entry != null) {
                    textSelectedDateInfo.setText("📅 Pe " + date + " ai făcut " + entry.steps + " pași.\n" +
                            "📏 Distanță: " + String.format(Locale.getDefault(), "%.2f", entry.distance) + " km\n" +
                            "🔥 Calorii: " + String.format(Locale.getDefault(), "%.0f", entry.calories) + " kcal");
                } else {
                    textSelectedDateInfo.setText("😴 Niciun pas înregistrat pe " + date + ".");
                }
            });
        }).start();
    }

    private String getFormattedDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
