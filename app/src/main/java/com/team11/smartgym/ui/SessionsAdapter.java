package com.team11.smartgym.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.SessionViewHolder> {

    private final List<Session> list = new ArrayList<>();

    public void submitList(List<Session> newList) {
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
        Session s = list.get(pos);

        String date = DateFormat.format("MMM dd, yyyy", s.startedAt).toString();
        String time = DateFormat.format("HH:mm", s.startedAt).toString();

        long durationMs = Math.max(0, s.endedAt - s.startedAt);
        long min = durationMs / 60000;
        long sec = (durationMs / 1000) % 60;

        h.tvDate.setText(date);
        h.tvTime.setText(time);
        h.tvDevice.setText("HR Sensor");
        h.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
        h.tvAvgHr.setText(String.valueOf(s.avgBpm));
        h.tvMaxHr.setText(String.valueOf(s.maxBpm));

        String summary = String.format(Locale.getDefault(),
                "Avg %d bpm  •  Max %d bpm", s.avgBpm, s.maxBpm);
        h.tvSummary.setText(summary);
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
