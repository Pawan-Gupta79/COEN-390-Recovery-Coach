package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.SessionRepository;

/**
 * Shows the list of sessions that belong to a specific workout. The UI mirrors SessionsFragment
 * but filters by workoutId.
 */
public class WorkoutSessionsFragment extends Fragment {

    private RecyclerView rvSessions;
    private View tvEmptyState;
    private SessionsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sessions, container, false);

        rvSessions = v.findViewById(R.id.rvSessions);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);

        TextView tvTitle = v.findViewById(R.id.tvSessionsTitle);
        // When viewing sessions for a specific workout, show "Activities" as the header.
        if (tvTitle != null) tvTitle.setText(R.string.sessions_title);

        adapter = new SessionsAdapter();
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
        rvSessions.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        long workoutId = -1L;
        Bundle args = getArguments();
        if (args != null) workoutId = args.getLong("workoutId", -1L);
        // capture as final for use inside observer lambdas
        final long wid = workoutId;

        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        if (workoutId > 0) {
            // Observe the sessions list; each session carries its own activity type.
            final java.util.concurrent.atomic.AtomicReference<java.util.List<com.team11.smartgym.data.Session>> sessionsHolder = new java.util.concurrent.atomic.AtomicReference<>();
            final String[] deviceHolder = new String[]{"Other"};

            androidx.lifecycle.LiveData<com.team11.smartgym.data.Workout> workoutLive = repo.getWorkoutLiveById(wid);
            workoutLive.observe(getViewLifecycleOwner(), workout -> {
                java.util.List<com.team11.smartgym.data.Session> s = sessionsHolder.get();
                if (s != null && !s.isEmpty()) {
                    // rebuild adapter with current device name
                    java.util.List<com.team11.smartgym.model.WorkoutSession> list = new java.util.ArrayList<>();
                    for (com.team11.smartgym.data.Session ss : s) {
                        int duration = (int) ((ss.endedAt - ss.startedAt) / 1000);
                        String deviceName = (ss.type == null || ss.type.isEmpty()) ? "Other" : ss.type;
                        list.add(new com.team11.smartgym.model.WorkoutSession(ss.id, deviceName, ss.startedAt, ss.endedAt, ss.avgBpm, ss.maxBpm, duration));
                    }
                    adapter.submitList(list);
                    rvSessions.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                }
            });

            repo.getSessionsForWorkoutLive(workoutId).observe(getViewLifecycleOwner(), sessions -> {
                if (sessions == null || sessions.isEmpty()) {
                    sessionsHolder.set(null);
                    adapter.submitList(null);
                    rvSessions.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    return;
                }
                sessionsHolder.set(sessions);
                // build using currently-known device name
                java.util.List<com.team11.smartgym.model.WorkoutSession> list = new java.util.ArrayList<>();
                for (com.team11.smartgym.data.Session s : sessions) {
                    int duration = (int) ((s.endedAt - s.startedAt) / 1000);
                    String deviceName = (s.type == null || s.type.isEmpty()) ? "Other" : s.type;
                    list.add(new com.team11.smartgym.model.WorkoutSession(s.id, deviceName, s.startedAt, s.endedAt, s.avgBpm, s.maxBpm, duration));
                }
                adapter.submitList(list);
                rvSessions.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
            });
        } else {
            // fallback: show nothing
            rvSessions.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        }

        return v;
    }
}
