package com.team11.smartgym.debug;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.team11.smartgym.R;

public class DebugLauncherActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_launcher);

        Button btnLiveHr = findViewById(R.id.btnOpenLiveHR);

        btnLiveHr.setOnClickListener(v -> {
            // Use class name string so release build compiles even if the class is debug-only
            Intent i = new Intent();
            i.setClassName(
                    getPackageName(),
                    "com.team11.smartgym.hr.ui.livehr.LiveHeartRateHostActivity"
            );
            startActivity(i);
        });
    }
}
