package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.team11.smartgym.R;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.ui.common.SnackbarUtil;

public class SettingsFragment extends Fragment {

    private MaterialSwitch switchAutoReconnect;
    private ConnectionViewModel vm;
    private AppPrefs prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        switchAutoReconnect = v.findViewById(R.id.switchAutoReconnect);
        vm    = new ViewModelProvider(requireActivity()).get(ConnectionViewModel.class);
        prefs = new AppPrefs(requireContext());

        // Initialize from prefs
        boolean on = prefs.isAutoReconnect();
        switchAutoReconnect.setChecked(on);
        vm.setAutoReconnectEnabled(on);

        // Write-through when toggled
        switchAutoReconnect.setOnCheckedChangeListener((button, isChecked) -> {
            prefs.setAutoReconnect(isChecked);
            vm.setAutoReconnectEnabled(isChecked);
            SnackbarUtil.show(v, isChecked
                    ? getString(R.string.auto_reconnect_on)
                    : getString(R.string.auto_reconnect_off));
        });
    }
}
