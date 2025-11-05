package com.team11.smartgym.hr.ui.livehr;

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

import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
import com.team11.smartgym.hr.domain.ConnectionState;
import com.team11.smartgym.hr.util.UiError;

public class LiveHeartRateFragment extends Fragment {

    private HeartRateViewModel vm;
    private View emptyBox;

    @Nullable
    @Override
        return inflater.inflate(R.layout.fragment_live_hr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tvBpm    = v.findViewById(R.id.tvBpm);
        tvUpdated= v.findViewById(R.id.tvUpdated);
        tvState  = v.findViewById(R.id.tvState);
        emptyBox = v.findViewById(R.id.emptyBox);
        btnStart = v.findViewById(R.id.btnStart);
        btnStop  = v.findViewById(R.id.btnStop);

        vm = new ViewModelProvider(this).get(HeartRateViewModel.class);

        btnStart.setOnClickListener(_v -> vm.start());
        btnStop.setOnClickListener(_v -> vm.stop());

        vm.bpm().observe(getViewLifecycleOwner(), bpm -> {
            boolean hasData = (bpm != null);
            tvBpm.setText(hasData ? String.valueOf(bpm) : "--");
            emptyBox.setVisibility(hasData ? View.GONE : View.VISIBLE);
        });


        vm.connection().observe(getViewLifecycleOwner(), state -> {
            tvState.setText(state == null ? "" : state.name());
        });

    }

        if (err.isBlocking) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setMessage(err.message)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } else {
        }
    }

    @Override public void onResume() {
        super.onResume();
        vm.start();
    }

    @Override public void onPause() {
        super.onPause();
        vm.stop();
    }
}
