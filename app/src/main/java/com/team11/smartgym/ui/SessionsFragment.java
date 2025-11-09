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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionRepository;
import com.team11.smartgym.ui.session.SessionViewModel;

import java.util.List;
import java.util.Locale;

public class SessionsFragment extends Fragment {

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
        return inflater.inflate(R.layout.fragment_sessions, container, false);
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
