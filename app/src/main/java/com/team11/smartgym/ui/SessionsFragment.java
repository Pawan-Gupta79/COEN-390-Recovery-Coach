
package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.model.WorkoutSession;
import com.team11.smartgym.ui.common.SnackbarUtil;

public class INSessionsFragment extends Fragment {

    private RecyclerView rvSessions;
    private LinearLayout tvEmptyState;
    private SessionsViewModel viewModel;
    private SessionsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.newfragment_sessions, container, false);

        rvSessions = v.findViewById(R.id.rvSessions);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);

        viewModel = new ViewModelProvider(this).get(SessionsViewModel.class);

        // Setup RecyclerView
        adapter = new SessionsAdapter(this::onSessionClick);
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
        rvSessions.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        );

        // Observe sessions data
        viewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.setSessions(sessions);

            // Show/hide empty state
            if (sessions == null || sessions.isEmpty()) {
                rvSessions.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                rvSessions.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
            }
        });

        // Observe errors
//        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
//            if (error != null && !error.isEmpty()) {
//                SnackbarUtil.show(requireView(), error);
//            }
//        });

        // Load sessions
        viewModel.loadSessions();

        return v;
    }

    /**
     * Handle session item click - prepare for detail view.
     * For now, just show a message. Can be expanded to navigate to detail fragment.
     */
    private void onSessionClick(WorkoutSession session) {
        String message = "Session selected: " + session.getId();
        SnackbarUtil.show(requireView(), message);

        // TODO: Navigate to session detail fragment
        // Bundle args = new Bundle();
        // args.putLong("session_id", session.getId());
        // NavHostFragment.findNavController(this)
        //     .navigate(R.id.action_sessions_to_detail, args);
    }
}
