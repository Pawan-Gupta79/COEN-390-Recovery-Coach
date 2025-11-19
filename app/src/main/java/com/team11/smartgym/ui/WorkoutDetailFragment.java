package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.Reading;
import com.team11.smartgym.data.SessionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only view for a saved Workout (shows sessions and summary).
 */
public class WorkoutDetailFragment extends Fragment {

    private TextView tvStatus, tvAvgBpm, tvMaxBpm, tvWorkoutStarted, tvBpm, tvTimer;
    private androidx.recyclerview.widget.RecyclerView rvWorkoutSessions;
    private SessionsAdapter sessionsAdapter;

    private SessionRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_workout, container, false);

        tvTimer = v.findViewById(R.id.tvTimer);
        tvStatus = v.findViewById(R.id.tvStatus);
        tvBpm = v.findViewById(R.id.tvBpm);
        tvAvgBpm = v.findViewById(R.id.tvAvgBpm);
        tvMaxBpm = v.findViewById(R.id.tvMaxBpm);
        tvWorkoutStarted = v.findViewById(R.id.tvWorkoutStarted);

        Button btnPause = v.findViewById(R.id.btnPause);
        Button btnCancel = v.findViewById(R.id.btnCancel);
        Button btnEnd = v.findViewById(R.id.btnEnd);

        // disable interactive controls
        btnPause.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);

        rvWorkoutSessions = v.findViewById(R.id.rvWorkoutSessions);
        sessionsAdapter = new SessionsAdapter();
        rvWorkoutSessions.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        rvWorkoutSessions.setAdapter(sessionsAdapter);

        try {
            repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        } catch (Exception e) {
            repo = null;
            Snackbar.make(v, "Database unavailable: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }

        Bundle args = getArguments();
        if (args != null && args.containsKey("workoutId") && repo != null) {
            long wid = args.getLong("workoutId", -1L);
            if (wid > 0) {
                repo.getWorkoutLiveById(wid).observe(getViewLifecycleOwner(), workout -> {
                    if (workout != null) {
                        tvStatus.setText("Completed");
                        tvBpm.setText("-- bpm");
                        tvAvgBpm.setText("Average: " + workout.avgBpm + " bpm");
                        tvMaxBpm.setText("Max: " + workout.maxBpm + " bpm");
                        java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
                        tvWorkoutStarted.setText(df.format(new java.util.Date(workout.startedAt)));
                    }
                });

                repo.getSessionsForWorkoutLive(wid).observe(getViewLifecycleOwner(), sessions -> {
                    if (sessions == null || sessions.isEmpty()) {
                        rvWorkoutSessions.setVisibility(View.GONE);
                        return;
                    }
                    List<com.team11.smartgym.model.WorkoutSession> list = new ArrayList<>();
                    for (com.team11.smartgym.data.Session s : sessions) {
                        int duration = (int) ((s.endedAt - s.startedAt) / 1000);
                        list.add(new com.team11.smartgym.model.WorkoutSession(
                                s.id, "Workout", s.startedAt, s.endedAt, s.avgBpm, s.maxBpm, duration
                        ));
                    }
                    sessionsAdapter.submitList(list);
                    rvWorkoutSessions.setVisibility(View.VISIBLE);
                });
            }
        }

        return v;
    }
}
