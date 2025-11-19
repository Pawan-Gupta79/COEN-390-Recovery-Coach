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

        adapter = new SessionsAdapter();
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
        rvSessions.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        long workoutId = -1L;
        Bundle args = getArguments();
        if (args != null) workoutId = args.getLong("workoutId", -1L);

        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        if (workoutId > 0) {
            repo.getSessionsForWorkoutLive(workoutId).observe(getViewLifecycleOwner(), sessions -> {
                adapter.submitList(sessions == null ? null : com.team11.smartgym.ui.transform.SessionTransform.toWorkoutSessionList(sessions));
                rvSessions.setVisibility((sessions == null || sessions.isEmpty()) ? View.GONE : View.VISIBLE);
                tvEmptyState.setVisibility((sessions == null || sessions.isEmpty()) ? View.VISIBLE : View.GONE);
            });
        } else {
            // fallback: show nothing
            rvSessions.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        }

        return v;
    }
}
