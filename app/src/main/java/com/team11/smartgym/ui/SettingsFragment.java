package com.team11.smartgym.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.team11.smartgym.BuildConfig;
import com.team11.smartgym.R;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.data.SessionManager;
import com.team11.smartgym.hr.ui.livehr.LiveHeartRateHostActivity;

public class SettingsFragment extends Fragment {

    private AppPrefs prefs;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = new AppPrefs(requireContext());
        session = new SessionManager(requireContext());

        MaterialSwitch switchAutoReconnect = v.findViewById(R.id.switchAutoReconnect);
        MaterialButton btnLogout = v.findViewById(R.id.btnLogout);
        MaterialButton btnDebug = v.findViewById(R.id.btnDebugLiveHr);

        // Load current preference state
        switchAutoReconnect.setChecked(prefs.isAutoReconnect());

        // Toggle auto-reconnect
        switchAutoReconnect.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setAutoReconnect(isChecked)
        );

        // Logout
        btnLogout.setOnClickListener(view -> {
            // 1) Clear session
            session.clear();

            // 2) Launch Login fresh task
            Intent i = new Intent(requireActivity(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);

            // 3) Finish this task stack to prevent back nav into Main
            requireActivity().finishAffinity();
        });

        // ---- DEBUG BUTTON (Visible only in debug build) ----
        if (btnDebug != null) {
            if (BuildConfig.DEBUG) {
                btnDebug.setVisibility(View.VISIBLE);
                btnDebug.setOnClickListener(view -> {
                    Intent intent = new Intent(requireContext(), LiveHeartRateHostActivity.class);
                    startActivity(intent);
                });
            } else {
                btnDebug.setVisibility(View.GONE);
            }
        }

        return v;
    }
}
