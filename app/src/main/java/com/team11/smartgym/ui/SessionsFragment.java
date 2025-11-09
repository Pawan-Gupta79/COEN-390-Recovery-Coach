
package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.model.WorkoutSession;
import com.team11.smartgym.ui.common.SnackbarUtil;

public class INSessionsFragment extends Fragment {
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionRepository;
import com.team11.smartgym.ui.session.SessionViewModel;

import java.util.List;
import java.util.Locale;

    private RecyclerView rvSessions;
    private LinearLayout tvEmptyState;
    private SessionsViewModel viewModel;
    private SessionsAdapter adapter;

    private RecyclerView recyclerView;
    private SessionsAdapter adapter;

    // mini controls
    private SessionViewModel vm;
    private TextView tvStateMini;
    private Button btnStartMini, btnStopMini, btnFakeMini;

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

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- list wiring ---
        recyclerView = view.findViewById(R.id.sessionsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SessionsAdapter();
        recyclerView.setAdapter(adapter);

        View empty = view.findViewById(R.id.layoutEmptyState);
        updateEmptyState(empty, null);

        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        repo.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.submitList(sessions);
            updateEmptyState(empty, sessions);
        });

        // --- mini controls wiring ---
        tvStateMini = view.findViewById(R.id.tvStateMini);
        btnStartMini = view.findViewById(R.id.btnStartMini);
        btnStopMini  = view.findViewById(R.id.btnStopMini);
        btnFakeMini  = view.findViewById(R.id.btnFakeMini);

        vm = new ViewModelProvider(this).get(SessionViewModel.class);

        vm.isRunning().observe(getViewLifecycleOwner(), running -> {
            boolean isRunning = running != null && running;
            tvStateMini.setText(isRunning ? "Running" : "Idle");
            btnStartMini.setEnabled(!isRunning);
            btnStopMini.setEnabled(isRunning);
            btnFakeMini.setEnabled(isRunning);
        });

        vm.liveSampleCount().observe(getViewLifecycleOwner(), count -> {
            Integer c = (count == null ? 0 : count);
            boolean isRunning = Boolean.TRUE.equals(vm.isRunning().getValue());
            String prefix = isRunning ? "Running" : "Idle";
            tvStateMini.setText(String.format(Locale.getDefault(), "%s • Samples: %d", prefix, c));
        });

        btnStartMini.setOnClickListener(v -> {
            Boolean running = vm.isRunning().getValue();
            if (running != null && running) return;
            vm.start();
        });

        btnStopMini.setOnClickListener(v -> {
            Boolean running = vm.isRunning().getValue();
            if (running == null || !running) return;
            vm.stop();
        });

        btnFakeMini.setOnClickListener(v -> {
            int bpm = 60 + (int)(Math.random() * 90); // 60–150
            vm.onHeartRate(bpm);
        });
    }

    private void updateEmptyState(@Nullable View emptyView, @Nullable List<Session> list) {
        boolean isEmpty = (list == null || list.isEmpty());
        if (emptyView != null) emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
