package com.team11.smartgym.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.WorkoutSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutsAdapter extends RecyclerView.Adapter<WorkoutsAdapter.WorkoutViewHolder> {

    private final List<WorkoutSummary> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(WorkoutSummary w); }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void submitList(List<WorkoutSummary> newList) {
        list.clear();
        if (newList != null) list.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new WorkoutViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder h, int pos) {
        WorkoutSummary w = list.get(pos);
        long startedAt = w.startedAt;
        String date = DateFormat.format("MMM dd, yyyy", startedAt).toString();
        String time = DateFormat.format("HH:mm", startedAt).toString();

        h.tvDate.setText(date);
        h.tvTime.setText(time);
        h.tvDevice.setText("Workout");
        int avg = w.avgBpm;
        int max = w.maxBpm;
        h.tvAvgHr.setText(String.format(Locale.getDefault(), "%d bpm", avg));
        h.tvMaxHr.setText(String.format(Locale.getDefault(), "%d bpm", max));
        if (w.endedAt > 0 && w.startedAt > 0) {
            long durMs = w.endedAt - w.startedAt;
            int sec = (int) (durMs / 1000);
            int m = sec / 60;
            int s = sec % 60;
            h.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
        } else {
            h.tvDuration.setText("--:--");
        }
        h.tvSummary.setText(String.format(Locale.getDefault(), "%d sessions", w.sessionCount));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(w);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static final class WorkoutViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate, tvTime, tvDevice, tvDuration, tvAvgHr, tvMaxHr, tvSummary;
        WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvSessionDate);
            tvTime = itemView.findViewById(R.id.tvSessionTime);
            tvDevice = itemView.findViewById(R.id.tvSessionDevice);
            tvDuration = itemView.findViewById(R.id.tvSessionDuration);
            tvAvgHr = itemView.findViewById(R.id.tvSessionAvgHr);
            tvMaxHr = itemView.findViewById(R.id.tvSessionMaxHr);
            tvSummary = itemView.findViewById(R.id.tvSessionSummary);
        }
    }
}
