package com.team11.smartgym.hr.ui.livehr;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;                // ANDROIDX
import androidx.fragment.app.FragmentManager;       // ANDROIDX
import androidx.fragment.app.FragmentTransaction;   // ANDROIDX

import com.team11.smartgym.R;

public class LiveHeartRateHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_hr_host);   // must contain @+id/container

        FragmentManager fm = getSupportFragmentManager(); // ANDROIDX
        Fragment existing = fm.findFragmentById(R.id.container);
        if (existing == null) {
            FragmentTransaction tx = fm.beginTransaction();
            Fragment fragment = new LiveHeartRateFragment(); // ensure this is ANDROIDX Fragment
            tx.replace(R.id.container, fragment, "LiveHR");  // replace(int, Fragment, tag)
            tx.commitNow();
        }
    }
}
