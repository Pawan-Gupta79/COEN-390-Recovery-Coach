package com.team11.smartgym.ui;

import android.content.Intent;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.team11.smartgym.R;
import com.team11.smartgym.data.SessionManager;
import com.team11.smartgym.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfig;
    private NavController navController;
    private SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Guard: if logged out, go to Login immediately
        if (!session.isLoggedIn()) {
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        NavHostFragment host =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host == null) return;
        navController = host.getNavController();

        appBarConfig = new AppBarConfiguration.Builder(
                R.id.dashboardFragment, R.id.sessionsFragment, R.id.settingsFragment
        ).build();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavigationUI.setupWithNavController(bottomNav, navController);
        bottomNav.setOnItemReselectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.dashboardFragment) {
                navController.popBackStack(R.id.dashboardFragment, false);
            } else if (id == R.id.sessionsFragment) {
                navController.popBackStack(R.id.sessionsFragment, false);
            } else if (id == R.id.settingsFragment) {
                navController.popBackStack(R.id.settingsFragment, false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Double-guard: if session was cleared while paused
        if (!new SessionManager(this).isLoggedIn()) {
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    }
}