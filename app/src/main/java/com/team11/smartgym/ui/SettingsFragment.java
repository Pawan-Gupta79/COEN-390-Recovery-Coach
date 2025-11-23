package com.team11.smartgym.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.team11.smartgym.BuildConfig;
import com.team11.smartgym.R;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.data.SessionManager;
import com.team11.smartgym.hr.ui.livehr.LiveHeartRateHostActivity;

public class SettingsFragment extends Fragment {

    private AppPrefs prefs;
    private SessionManager session;

    private TextView login, viewProfile, editProfile;
    private MaterialSwitch switchAutoReconnect;
    private MaterialButton btnLogout;
    private MaterialButton btnDebug;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = new AppPrefs(requireContext());
        session = new SessionManager(requireContext());

        // Find views
        login       = v.findViewById(R.id.login);
        viewProfile = v.findViewById(R.id.viewProfile);
        editProfile = v.findViewById(R.id.editProfile);
        switchAutoReconnect = v.findViewById(R.id.switchAutoReconnect);
        btnLogout   = v.findViewById(R.id.btnLogout);
        btnDebug    = v.findViewById(R.id.btnDebugLiveHr);

        NavController navController =
                Navigation.findNavController(requireActivity(), R.id.nav_host);

        // ---- Auto reconnect toggle ----
        switchAutoReconnect.setChecked(prefs.isAutoReconnect());
        switchAutoReconnect.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setAutoReconnect(isChecked)
        );

        // ---- Login / profile visibility based on session ----
        boolean loggedIn = isUserLoggedIn();

        if (loggedIn) {
            // Logged in: show profile, show logout, hide login
            login.setVisibility(View.GONE);
            viewProfile.setVisibility(View.VISIBLE);
            editProfile.setVisibility(View.VISIBLE);
            if (btnLogout != null) btnLogout.setVisibility(View.VISIBLE);
        } else {
            // Not logged in: show login, hide profile + logout
            login.setVisibility(View.VISIBLE);
            viewProfile.setVisibility(View.GONE);
            editProfile.setVisibility(View.GONE);
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
        }

        // ---- Login: always go to LoginActivity ----
        login.setOnClickListener(view -> {
            Intent i = new Intent(requireActivity(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            requireActivity().finishAffinity();
        });

        // ---- Edit profile ----
        editProfile.setOnClickListener(view -> {
            if (isUserLoggedIn()) {
                navController.navigate(R.id.action_settingsFragment_to_editProfileFragment);
            } else {
                Toast.makeText(requireContext(),
                        "Please log in to edit your profile.", Toast.LENGTH_SHORT).show();
            }
        });

        // ---- View profile ----
        viewProfile.setOnClickListener(view -> {
            if (isUserLoggedIn()) {
                navController.navigate(R.id.action_settingsFragment_to_viewProfileFragment);
            } else {
                Toast.makeText(requireContext(),
                        "Please log in to view your profile.", Toast.LENGTH_SHORT).show();
            }
        });

        // ---- Single Logout button ----
        if (btnLogout != null) {
            btnLogout.setOnClickListener(view -> {
                if (isUserLoggedIn()) {
                    showLogoutConfirmation();
                } else {
                    Toast.makeText(requireContext(),
                            "You are already logged out.", Toast.LENGTH_SHORT).show();
                }
            });
        }

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

    /**
     * Show confirmation dialog before logging out.
     */
    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout, (dialog, which) -> performLogout())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Perform the logout operation and return to LoginActivity.
     */
    private void performLogout() {
        // Clear SessionManager flags
        session.clear();

        // Clear user_session SharedPreferences used across app
        SharedPreferences prefsSp =
                requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        prefsSp.edit().clear().apply();

        Toast.makeText(requireContext(),
                "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // Go back to LoginActivity and clear back stack
        Intent i = new Intent(requireActivity(), LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finishAffinity();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences userPrefs =
                requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int userId = userPrefs.getInt("user_id", -1);
        return userId != -1;
    }
}
