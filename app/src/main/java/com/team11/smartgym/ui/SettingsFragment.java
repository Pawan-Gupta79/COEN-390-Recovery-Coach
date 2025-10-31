package com.team11.smartgym.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.team11.smartgym.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;
    private SharedPreferences prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSettingsBinding.inflate(inflater, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        b.switchAutoReconnect.setChecked(prefs.getBoolean("auto_reconnect", true));
        b.switchAutoReconnect.setOnCheckedChangeListener((v, on) ->
                prefs.edit().putBoolean("auto_reconnect", on).apply());
        return b.getRoot();
    }
}