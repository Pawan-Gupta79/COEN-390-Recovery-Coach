package com.team11.smartgym.hr.ui.livehr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;   // << ANDROIDX Fragment

import com.team11.smartgym.R;

// Make sure we extend the ANDROIDX Fragment
public class LiveHeartRateFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_hr, container, false);
    }

    // ... keep the rest of your code exactly as you have it ...
}
