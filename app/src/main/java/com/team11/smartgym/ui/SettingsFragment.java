package com.team11.smartgym.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.team11.smartgym.R;
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

        // Go to login page
        b.login.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentHost, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Go to edit profile page
        b.editProfile.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentHost, new EditProfileFragment())
                        .addToBackStack(null)
                        .commit();
            }
            else {
                Toast.makeText(requireContext(), "Please log in to edit your profile.", Toast.LENGTH_SHORT).show();
            }
        });

        // Go to profile page
        b.viewProfile.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentHost, new ViewProfileFragment())
                        .addToBackStack(null)
                        .commit();
            }
            else {
                Toast.makeText(requireContext(), "Please log in to view your profile.", Toast.LENGTH_SHORT).show();
            }
        });

        b.logout.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
                prefs.edit().remove("user_id").apply();
                Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(requireContext(), "You are already logged out.", Toast.LENGTH_SHORT).show();
            }

        });

        return b.getRoot();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences userPrefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int userId = userPrefs.getInt("user_id", -1);
        return userId != -1;
    }
}