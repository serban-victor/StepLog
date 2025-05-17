package com.example.steplog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimedSessionAdapter extends RecyclerView.Adapter<TimedSessionAdapter.ViewHolder> {

    private List<TimedSessionEntry> timedSessions;

    public TimedSessionAdapter(List<TimedSessionEntry> timedSessions) {
        this.timedSessions = timedSessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timed_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimedSessionEntry session = timedSessions.get(position);

        holder.tvSessionSteps.setText(String.format(Locale.getDefault(), "Steps: %d", session.steps));
        holder.tvSessionDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", session.distanceKm));
        holder.tvSessionCalories.setText(String.format(Locale.getDefault(), "Calories: %.1f kcal", session.caloriesKcal));

        // Format duration from milliseconds to HH:mm:ss or mm:ss
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
        holder.tvSessionDuration.setText(String.format(Locale.getDefault(), "Duration: %s", durationFormatted));

        // Format start time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTimeFormatted = timeFormat.format(new Date(session.startTime));
        holder.tvSessionStartTime.setText(String.format(Locale.getDefault(), "Started at: %s", startTimeFormatted));
    }

    @Override
    public int getItemCount() {
        return timedSessions.size();
    }

    public void updateSessions(List<TimedSessionEntry> newSessions) {
        this.timedSessions.clear();
        if (newSessions != null) {
            this.timedSessions.addAll(newSessions);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSessionSteps, tvSessionDistance, tvSessionCalories, tvSessionDuration, tvSessionStartTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvSessionSteps = itemView.findViewById(R.id.tvItemSessionSteps);
            tvSessionDistance = itemView.findViewById(R.id.tvItemSessionDistance);
            tvSessionCalories = itemView.findViewById(R.id.tvItemSessionCalories);
            tvSessionDuration = itemView.findViewById(R.id.tvItemSessionDuration);
            tvSessionStartTime = itemView.findViewById(R.id.tvItemSessionStartTime);
        }
    }
}

