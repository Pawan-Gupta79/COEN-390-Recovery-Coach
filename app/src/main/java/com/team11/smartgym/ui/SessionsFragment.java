package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.SessionRepository;

public class SessionsFragment extends Fragment {

    private RecyclerView rvSessions;
    private View tvEmptyState;

    private SessionsAdapter adapter;
    private WorkoutsAdapter workoutsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sessions, container, false);

        rvSessions = v.findViewById(R.id.rvSessions);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);

        Button btnOverview = v.findViewById(R.id.btn_overview);
        btnOverview.setOnClickListener(view -> {
            Navigation.findNavController(view).navigate(R.id.action_sessions_to_overview);
        });

        // Adapter (we show workouts grouped list)
        adapter = new SessionsAdapter();
        workoutsAdapter = new WorkoutsAdapter();
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(workoutsAdapter);
        rvSessions.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        // Repository
        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();

        // Observe workout summaries (includes session counts) and update UI
        repo.getAllWorkoutSummariesLive().observe(getViewLifecycleOwner(), summaries -> {
            workoutsAdapter.submitList(summaries);
            rvSessions.setVisibility((summaries == null || summaries.isEmpty()) ? View.GONE : View.VISIBLE);
            tvEmptyState.setVisibility((summaries == null || summaries.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        workoutsAdapter.setOnItemClickListener(w -> {
            Bundle b = new Bundle();
            b.putLong("workoutId", w.id);
            androidx.navigation.Navigation.findNavController(rvSessions).navigate(R.id.action_sessions_to_workout, b);
        });

        return v;
    }
}
