
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.team11.smartgym.R;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.data.SessionManager;

public class INSettingsFragment extends Fragment {

    private AppPrefs prefs;
    private SessionManager session;

    private String logout_confirmation_title = "Logout Confirmed";
    private String logout_confirmation_message = "Logout is confirmed.";


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

        // Load current preference state
        switchAutoReconnect.setChecked(prefs.isAutoReconnect());

        // Toggle auto-reconnect
        switchAutoReconnect.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setAutoReconnect(isChecked)
        );

        // Logout with confirmation dialog
        btnLogout.setOnClickListener(view -> showLogoutConfirmation());

        return v;
    }

    /**
     * Show confirmation dialog before logging out
     */
    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton(R.string.logout, (dialog, which) -> performLogout())
                .setNegativeButton("cancel", null)
                .show();
    }

    /**
     * Perform the logout operation
     */
    private void performLogout() {
        // 1) Clear session
        session.clear();

        // 2) Optionally clear last device to avoid auto-reconnect surprises
        prefs.clearLastDevice();

        // 3) Launch Login activity with fresh task
        Intent i = new Intent(requireActivity(), LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);

        // 4) Finish this task stack to prevent back navigation into Main
        requireActivity().finishAffinity();
    }
}
