package com.team11.smartgym.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.model.WorkoutSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of WorkoutSession items.
 */
public class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.SessionViewHolder> {

    private final List<WorkoutSession> list = new ArrayList<>();

    public void submitList(List<WorkoutSession> newList) {
        list.clear();
        if (newList != null) list.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder h, int pos) {
        WorkoutSession s = list.get(pos);

        long startedAt = s.getStartedAt();
        long endedAt   = s.getEndedAt();

        String date = DateFormat.format("MMM dd, yyyy", startedAt).toString();
        String time = DateFormat.format("HH:mm", startedAt).toString();

        h.tvDate.setText(date);
        h.tvTime.setText(time);

        String deviceName = s.getDeviceName();
        h.tvDevice.setText(deviceName == null || deviceName.isEmpty()
                ? "HR Sensor"
                : deviceName);

        // Duration
        h.tvDuration.setText(s.getFormattedDuration());

        // Heart rates
        h.tvAvgHr.setText(String.format(Locale.getDefault(), "%d bpm", s.getAvgHeartRate()));
        h.tvMaxHr.setText(String.format(Locale.getDefault(), "%d bpm", s.getMaxHeartRate()));

        // Optional: summary field removed or repurposed
        h.tvSummary.setText(""); // or remove this view in XML
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static final class SessionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate, tvTime, tvDevice, tvDuration, tvAvgHr, tvMaxHr, tvSummary;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate     = itemView.findViewById(R.id.tvSessionDate);
            tvTime     = itemView.findViewById(R.id.tvSessionTime);
            tvDevice   = itemView.findViewById(R.id.tvSessionDevice);
            tvDuration = itemView.findViewById(R.id.tvSessionDuration);
            tvAvgHr    = itemView.findViewById(R.id.tvSessionAvgHr);
            tvMaxHr    = itemView.findViewById(R.id.tvSessionMaxHr);
            tvSummary  = itemView.findViewById(R.id.tvSessionSummary);
        }
    }
}
