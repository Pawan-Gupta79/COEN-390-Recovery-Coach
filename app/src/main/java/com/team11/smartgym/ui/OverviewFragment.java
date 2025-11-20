package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.team11.smartgym.R;
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionRepository;

import java.util.List;

public class OverviewFragment extends Fragment {

    public OverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host);
            navController.navigateUp();
        });

        LinearLayout clickableTitle1 = view.findViewById(R.id.clickable_title_1);
        LinearLayout clickableTitle2 = view.findViewById(R.id.clickable_title_2);
        LinearLayout clickableTitle3 = view.findViewById(R.id.clickable_title_3);

        TextView tvAvgHeartRate = view.findViewById(R.id.tv_avg_heart_rate);

        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        repo.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null && !sessions.isEmpty()) {
                int totalBpm = 0;
                int sessionCount = 0;
                for (Session session : sessions) {
                    if (session.avgBpm > 0) {
                        totalBpm += session.avgBpm;
                        sessionCount++;
                    }
                }
                if (sessionCount > 0) {
                    tvAvgHeartRate.setText(String.valueOf(totalBpm / sessionCount));
                }
            }
        });

        clickableTitle1.setOnClickListener(v -> {
            Toast.makeText(getContext(), "First item clicked", Toast.LENGTH_SHORT).show();
        });

        clickableTitle2.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Second item clicked", Toast.LENGTH_SHORT).show();
        });

        clickableTitle3.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Third item clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((AppCompatActivity) requireActivity()).setSupportActionBar(null);
    }
}