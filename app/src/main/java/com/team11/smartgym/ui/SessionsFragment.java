package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;

public class SessionsFragment extends Fragment {

    private RecyclerView rvSessions;
    private View tvEmptyState;

    private SessionsViewModel viewModel;
    private SessionsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Use your existing layout for the sessions screen
        View v = inflater.inflate(R.layout.fragment_sessions, container, false);

        rvSessions = v.findViewById(R.id.rvSessions);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(SessionsViewModel.class);

        // RecyclerView + adapter
        adapter = new SessionsAdapter();
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
        rvSessions.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        );

        // Observe sessions list
        viewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.submitList(sessions);

            if (sessions == null || sessions.isEmpty()) {
                rvSessions.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                rvSessions.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
            }
        });

        // Trigger load
        viewModel.loadSessions();

        return v;
    }
}
