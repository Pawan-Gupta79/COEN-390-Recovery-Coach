package com.team11.smartgym.debug;

import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class WorkoutSessionView extends LinearLayout {

    public interface OnDeleteListener {
        void onDelete(int index);
    }

    private TextView tvSummary;
    private Button btnDelete;

    public WorkoutSessionView(Context context) {
        super(context);

        setOrientation(HORIZONTAL);
        setPadding(16, 16, 16, 16);
        setGravity(Gravity.CENTER_VERTICAL);

        tvSummary = new TextView(context);
        LayoutParams tvParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        tvSummary.setLayoutParams(tvParams);

        btnDelete = new Button(context);
        btnDelete.setText("Delete");

        addView(tvSummary);
        addView(btnDelete);
    }

    public void bind(WorkoutSession session, int index, OnDeleteListener listener) {
        String summary = String.format(Locale.getDefault(),
                "Duration: %s | Steps: %d | Avg HR: %.1f | Calories: %.1f",
                formatDuration(session.durationMs),
                session.steps,
                session.avgHr,
                session.calories);

        tvSummary.setText(summary);

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(index);
            }
        });
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d",
                    hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d",
                    minutes, seconds);
        }
    }
}
