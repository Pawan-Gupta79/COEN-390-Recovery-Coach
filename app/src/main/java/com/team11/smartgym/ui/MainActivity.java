package com.team11.smartgym.ui;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;
import com.team11.smartgym.R;
import com.team11.smartgym.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        requestRuntimePerms();

        b.bottomNav.setOnItemSelectedListener(navListener);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentHost, new DashboardFragment())
                    .commit();
        }
    }

    private final NavigationBarView.OnItemSelectedListener navListener = item -> {
        Fragment f;
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) f = new DashboardFragment();
        else if (id == R.id.nav_sessions) f = new SessionsFragment();
        else f = new SettingsFragment();

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentHost, f)
                .commit();
        return true;
    };

    private void requestRuntimePerms() {
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            }, 1);
        } else {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 1);
        }
    }
}