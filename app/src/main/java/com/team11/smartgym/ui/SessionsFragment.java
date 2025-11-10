package com.team11.smartgym.ui;
// maybe the new github code will work
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionRepository;
import com.team11.smartgym.ui.common.SnackbarUtil;
import com.team11.smartgym.ui.session.SessionViewModel;

import java.util.List;
import java.util.Locale;

public class SessionsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private SessionsAdapter adapter;
    private SessionViewModel vm;

    // Mini controls
    private TextView tvStateMini;
    private Button btnStartMini, btnStopMini, btnFakeMini;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_sessions, container, false);

        // Fixed IDs to match XML
        recyclerView = v.findViewById(R.id.sessionsRecyclerView);
        emptyState = v.findViewById(R.id.layoutEmptyState);
        tvStateMini = v.findViewById(R.id.tvStateMini);
        btnStartMini = v.findViewById(R.id.btnStartMini);
        btnStopMini = v.findViewById(R.id.btnStopMini);
        btnFakeMini = v.findViewById(R.id.btnFakeMini);

        // Recycler setup
      adapter = new SessionsAdapters(this::onSessionClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        );

        // Observe sessions from database
        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        repo.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.submitList(sessions);
            updateEmptyState(sessions);
        });

        // Mini session controls ViewModel
        vm = new ViewModelProvider(this).get(SessionViewModel.class);

        vm.isRunning().observe(getViewLifecycleOwner(), running -> {
            boolean isRunning = running != null && running;
            tvStateMini.setText(isRunning ? "Running" : "Idle");
            btnStartMini.setEnabled(!isRunning);
            btnStopMini.setEnabled(isRunning);
            btnFakeMini.setEnabled(isRunning);
        });

        vm.liveSampleCount().observe(getViewLifecycleOwner(), count -> {
            int c = (count == null) ? 0 : count;
            boolean isRunning = Boolean.TRUE.equals(vm.isRunning().getValue());
            String prefix = isRunning ? "Running" : "Idle";
            tvStateMini.setText(String.format(Locale.getDefault(), "%s • Samples: %d", prefix, c));
        });

        // Mini control buttons
        btnStartMini.setOnClickListener(view -> {
            if (Boolean.TRUE.equals(vm.isRunning().getValue())) return;
            vm.start();
        });

        btnStopMini.setOnClickListener(view -> {
            if (!Boolean.TRUE.equals(vm.isRunning().getValue())) return;
            vm.stop();
        });

        btnFakeMini.setOnClickListener(view -> {
            int bpm = 60 + (int) (Math.random() * 90); // 60–150
            vm.onHeartRate(bpm);
        });

        return v;
    }

    private void onSessionClick(Session session) {
        String message = "Session selected: " + session.getId();
        SnackbarUtil.show(requireView(), message);
    }

    private void updateEmptyState(@Nullable List<Session> list) {
        boolean isEmpty = (list == null || list.isEmpty());
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
