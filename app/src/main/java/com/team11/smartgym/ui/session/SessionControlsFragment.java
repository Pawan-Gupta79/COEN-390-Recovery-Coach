package com.team11.smartgym.ui.session;

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
import androidx.navigation.fragment.NavHostFragment;

import com.team11.smartgym.R;
import com.team11.smartgym.data.TempSessionSnapshot;

import java.util.Locale;

/**
 * Minimal UI to start/stop a session and show the last saved snapshot,
 * plus a button to navigate to the dedicated Snapshot viewer.
 */
public class SessionControlsFragment extends Fragment {

    private SessionViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_controls, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        vm = new ViewModelProvider(this).get(SessionViewModel.class);

        Button btnStart = v.findViewById(R.id.btnStartSession);
        Button btnStop  = v.findViewById(R.id.btnStopSession);
        Button btnFake  = v.findViewById(R.id.btnFakeSample);
        Button btnOpenSnapshot = v.findViewById(R.id.btnOpenSnapshot);
        TextView tvState = v.findViewById(R.id.tvState);
        TextView tvLast  = v.findViewById(R.id.tvLastSnapshot);

        // Observers
        vm.isRunning().observe(getViewLifecycleOwner(), running -> {
            boolean isRunning = running != null && running;
            tvState.setText(isRunning ? "Running" : "Idle");
            btnStart.setEnabled(!isRunning);
            btnStop.setEnabled(isRunning);
            btnFake.setEnabled(isRunning);
        });

        vm.lastSnapshot().observe(getViewLifecycleOwner(), snap -> {
            tvLast.setText(formatSnapshot(snap));
        });

        // Actions (fixed: use start() / stop())
        btnStart.setOnClickListener(view -> vm.start());
        btnStop.setOnClickListener(view -> vm.stop());

        // Fake sample for quick testing without BLE (optional)
        btnFake.setOnClickListener(view -> {
            int bpm = 60 + (int)(Math.random() * 90); // 60–150
            vm.onHeartRate(bpm);
        });

        // Navigate to Snapshot viewer
        btnOpenSnapshot.setOnClickListener(view ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_controls_to_snapshot)
        );
    }

    private String formatSnapshot(@Nullable TempSessionSnapshot s) {
        if (s == null) return "No snapshot saved.";
        String idShort = (s.tempId == null) ? "-" :
                (s.tempId.length() > 8 ? s.tempId.substring(0, 8) : s.tempId);
        String stats = (s.stats == null) ? "-" :
                String.format(Locale.getDefault(), "avg=%d, max=%d, invalid=%s",
                        s.stats.averageBpm, s.stats.maxBpm, s.stats.invalid);
        return "ID " + idShort +
                "\nStart: " + s.startMs +
                "\nEnd:   " + s.endMs +
                "\nSamples: " + s.samplesCount +
                "\nStats: " + stats;
    }
}
