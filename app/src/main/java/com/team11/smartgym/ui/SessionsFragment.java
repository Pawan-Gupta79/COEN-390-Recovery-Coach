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
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.SessionRepository;

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

        View v = inflater.inflate(R.layout.fragment_sessions, container, false);

        rvSessions = v.findViewById(R.id.rvSessions);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);

        // Adapter
        adapter = new SessionsAdapter();
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(adapter);
        rvSessions.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        // Repository + ViewModel
        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        viewModel = new ViewModelProvider(this, new SessionsViewModelFactory(repo))
                .get(SessionsViewModel.class);

        // Observe
        viewModel.getSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.submitList(sessions);
            rvSessions.setVisibility((sessions == null || sessions.isEmpty()) ? View.GONE : View.VISIBLE);
            tvEmptyState.setVisibility((sessions == null || sessions.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        return v;
    }
}
