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

import com.team11.smartgym.R;
import com.team11.smartgym.data.SnapshotProvider;

/**
 * Read-only viewer for the last TempSessionSnapshot saved by DS-01.7.
 * Safe to drop in alongside your existing screens. No DB writes.
 */
public class SessionsSnapshotFragment extends Fragment {

    private SnapshotProvider provider;
    private TextView tvDetails;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sessions_snapshot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        provider = new SnapshotProvider(requireContext());

        tvDetails = v.findViewById(R.id.tvSnapshotDetails);
        Button btnRefresh = v.findViewById(R.id.btnRefreshSnapshot);
        Button btnClear   = v.findViewById(R.id.btnClearSnapshot);

        btnRefresh.setOnClickListener(view -> render());
        btnClear.setOnClickListener(view -> {
            provider.clear();
            render();
        });

        render();
    }

    private void render() {
        tvDetails.setText(provider.loadFormatted());
    }
}
