
package com.team11.smartgym.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.team11.smartgym.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;
    private SharedPreferences sprefs;

    private AppPrefs prefs;
    private SessionManager session;

    TextView login, viewProfile, editProfile;
    Button logout;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = new AppPrefs(requireContext());
        session = new SessionManager(requireContext());
        login = v.findViewById(R.id.login);
        viewProfile = v.findViewById(R.id.viewProfile);
        editProfile = v.findViewById(R.id.editProfile);
        logout = v.findViewById(R.id.logout);
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host);
        MaterialSwitch switchAutoReconnect = v.findViewById(R.id.switchAutoReconnect);
        MaterialButton btnLogout = v.findViewById(R.id.btnLogout);
        MaterialButton btnDebug = v.findViewById(R.id.btnDebugLiveHr);

        // Load current preference state
        switchAutoReconnect.setChecked(prefs.isAutoReconnect());

        // Toggle auto-reconnect
        switchAutoReconnect.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setAutoReconnect(isChecked)
        );

        // Go to login page
        login.setOnClickListener(view -> {
            navController.navigate(R.id.action_settingsFragment_to_loginFragment);
        });

        // Go to edit profile page
        editProfile.setOnClickListener(view -> {
            if (isUserLoggedIn()) {
                navController.navigate(R.id.action_settingsFragment_to_editProfileFragment);
            }
            else {
                Toast.makeText(requireContext(), "Please log in to edit your profile.", Toast.LENGTH_SHORT).show();
            }
        });

        // Go to profile page
        viewProfile.setOnClickListener(view -> {
            if (isUserLoggedIn()) {
                navController.navigate(R.id.action_settingsFragment_to_viewProfileFragment);
            }
            else {
                Toast.makeText(requireContext(), "Please log in to view your profile.", Toast.LENGTH_SHORT).show();
            }
        });
        //logout user db
        logout.setOnClickListener(view -> {
                    if (isUserLoggedIn()) {
                        SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
                        prefs.edit().remove("user_id").apply();
                        Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "You are already logged out.", Toast.LENGTH_SHORT).show();
                    }
        });

        // Logout
        btnLogout.setOnClickListener(view -> {
            // 1) Clear session
            session.clear();

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


    private boolean isUserLoggedIn() {
        SharedPreferences userPrefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int userId = userPrefs.getInt("user_id", -1);
        return userId != -1;
    }
}