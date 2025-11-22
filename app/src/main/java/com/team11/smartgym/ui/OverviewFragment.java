package com.team11.smartgym.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.team11.smartgym.R;
import com.team11.smartgym.data.DatabaseProvider;
import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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

        TextView avgHr3Sessions = view.findViewById(R.id.avg_hr_3_sessions);
        TextView avgDuration3Sessions = view.findViewById(R.id.avg_duration_3_sessions);
        TextView avgHr5Sessions = view.findViewById(R.id.avg_hr_5_sessions);
        TextView avgDuration5Sessions = view.findViewById(R.id.avg_duration_5_sessions);
        TextView avgHr10Sessions = view.findViewById(R.id.avg_hr_10_sessions);
        TextView avgDuration10Sessions = view.findViewById(R.id.avg_duration_10_sessions);
        BarChart barChart = view.findViewById(R.id.bar_chart);

        SessionRepository repo = DatabaseProvider.get(requireContext()).getSessionRepository();
        repo.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null && !sessions.isEmpty()) {
                updateSessionStats(sessions, 3, avgHr3Sessions, avgDuration3Sessions);
                updateSessionStats(sessions, 5, avgHr5Sessions, avgDuration5Sessions);
                updateSessionStats(sessions, 10, avgHr10Sessions, avgDuration10Sessions);
                setupBarChart(barChart, sessions);
            }
        });
    }

    private void updateSessionStats(List<Session> sessions, int count, TextView avgHrView, TextView avgDurationView) {
        int totalBpm = 0;
        long totalDuration = 0;

        int numberOfSessions = Math.min(sessions.size(), count);
        if (numberOfSessions == 0) return;

        int startIndex = sessions.size() - numberOfSessions;
        for (int i = startIndex; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            totalBpm += session.avgBpm;
            totalDuration += (session.endedAt - session.startedAt);
        }

        avgHrView.setText(String.format(Locale.getDefault(), "Avg HR: %d", totalBpm / numberOfSessions));
        long avgDurationMillis = totalDuration / numberOfSessions;
        String formattedDuration = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(avgDurationMillis),
                TimeUnit.MILLISECONDS.toMinutes(avgDurationMillis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(avgDurationMillis) % TimeUnit.MINUTES.toSeconds(1));
        avgDurationView.setText(String.format(Locale.getDefault(), "Avg Duration: %s", formattedDuration));
    }

    private void setupBarChart(BarChart barChart, List<Session> sessions) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int numberOfSessions = Math.min(sessions.size(), 5);
        int startIndex = sessions.size() - numberOfSessions;

        for (int i = 0; i < numberOfSessions; i++) {
            Session session = sessions.get(startIndex + i);
            entries.add(new BarEntry(i, session.avgBpm));
            labels.add(String.valueOf(i + 1)); // Corrected Labeling
        }

        BarDataSet dataSet = new BarDataSet(entries, "Average Heart Rate");
        dataSet.setColor(Color.BLUE);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false);

        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setTouchEnabled(false);

        barChart.setFitBars(true);
        barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((AppCompatActivity) requireActivity()).setSupportActionBar(null);
    }
}