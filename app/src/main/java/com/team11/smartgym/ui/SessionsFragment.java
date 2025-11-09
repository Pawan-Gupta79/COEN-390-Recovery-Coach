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
import androidx.navigation.fragment.NavHostFragment;

import com.team11.smartgym.R;
import com.team11.smartgym.data.SnapshotProvider;

public class SessionsFragment extends Fragment {

    private SnapshotProvider snapshotProvider;
    private TextView tvSnapshot;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sessions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        snapshotProvider = new SnapshotProvider(requireContext());
        tvSnapshot = v.findViewById(R.id.tvSessionsSnapshot);

        Button btnRefresh = v.findViewById(R.id.btnSessionsRefresh);
        Button btnOpenControls = v.findViewById(R.id.btnOpenControls);
        Button btnOpenViewer = v.findViewById(R.id.btnOpenViewer);
        Button btnClear = v.findViewById(R.id.btnSessionsClear);

        btnRefresh.setOnClickListener(view -> render());
        btnOpenControls.setOnClickListener(view ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_sessions_to_sessionControls)
        );
        btnOpenViewer.setOnClickListener(view ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_sessions_to_snapshot)
        );
        btnClear.setOnClickListener(view -> {
            snapshotProvider.clear();
            render();
        });

        render();
    }

    private void render() {
        tvSnapshot.setText(snapshotProvider.loadAllFormatted());
    }
}
