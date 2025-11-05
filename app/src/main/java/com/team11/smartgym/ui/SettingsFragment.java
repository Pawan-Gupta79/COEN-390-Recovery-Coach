package com.team11.smartgym.ui;

import android.content.ActivityNotFoundException;
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
import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.BuildConfig;
import com.team11.smartgym.R;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.data.SessionManager;

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
        MaterialButton btnDebug = v.findViewById(R.id.btnDebugTools);

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

        // ---- Debug tools (debug builds only) ----
        if (BuildConfig.DEBUG) {
            btnDebug.setVisibility(View.VISIBLE);
            btnDebug.setOnClickListener(view -> {
                try {
                    Intent i = new Intent();
                    i.setClassName(
                            requireContext().getPackageName(),
                            "com.team11.smartgym.hr.ui.livehr.LiveHeartRateHostActivity"
                    );
                    startActivity(i);
                } catch (ActivityNotFoundException ex) {
                    Snackbar.make(v, "Live HR debug screen not available.", Snackbar.LENGTH_SHORT).show();
                }
            });
        } else {
            btnDebug.setVisibility(View.GONE);
        }


        return v;
    }
}
