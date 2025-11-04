package com.team11.smartgym.hr.ui.livehr;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.team11.smartgym.R;

public class LiveHeartRateHostActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_hr_host);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new LiveHeartRateFragment())
                    .commit();
        }

        setTitle(R.string.title_live_hr);
    }
}
