package com.team11.smartgym.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.model.WorkoutSession;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying workout sessions in a RecyclerView.
 */
public class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.SessionViewHolder> {

    private List<WorkoutSession> sessions = new ArrayList<>();
    private final OnSessionClickListener clickListener;
    private final DateFormat dateFormat;
    private final DateFormat timeFormat;

    public interface OnSessionClickListener {
        void onSessionClick(WorkoutSession session);
    }

    public SessionsAdapter(OnSessionClickListener clickListener) {
        this.clickListener = clickListener;
        this.dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        this.timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
    }

    public void setSessions(List<WorkoutSession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        WorkoutSession session = sessions.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate;
        private final TextView tvTime;
        private final TextView tvDevice;
        private final TextView tvDuration;
        private final TextView tvAvgHr;
        private final TextView tvMaxHr;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvSessionDate);
            tvTime = itemView.findViewById(R.id.tvSessionTime);
            tvDevice = itemView.findViewById(R.id.tvSessionDevice);
            tvDuration = itemView.findViewById(R.id.tvSessionDuration);
            tvAvgHr = itemView.findViewById(R.id.tvSessionAvgHr);
            tvMaxHr = itemView.findViewById(R.id.tvSessionMaxHr);
        }

        void bind(WorkoutSession session) {
            Date startDate = new Date(session.getStartedAt());

            tvDate.setText(dateFormat.format(startDate));
            tvTime.setText(timeFormat.format(startDate));
            tvDevice.setText(session.getDeviceName());
            tvDuration.setText(session.getFormattedDuration());
            tvAvgHr.setText(String.valueOf(session.getAvgHeartRate()));
            tvMaxHr.setText(String.valueOf(session.getMaxHeartRate()));

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSessionClick(session);
                }
            });
        }
    }
}