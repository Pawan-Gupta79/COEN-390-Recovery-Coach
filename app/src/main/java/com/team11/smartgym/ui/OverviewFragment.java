package com.team11.smartgym.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.team11.smartgym.R;
import com.team11.smartgym.data.WorkoutSession;

import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    private TextView avg1DayView, avg3DayView, avg7DayView;
    private TextView count1DayView, count3DayView, count7DayView;
    private SessionsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_overview, container, false);

        avg1DayView = root.findViewById(R.id.avg_1day);
        avg3DayView = root.findViewById(R.id.avg_3day);
        avg7DayView = root.findViewById(R.id.avg_7day);

        count1DayView = root.findViewById(R.id.count_1day);
        count3DayView = root.findViewById(R.id.count_3day);
        count7DayView = root.findViewById(R.id.count_7day);

        Button btnBack = root.findViewById(R.id.btnBackOverview);
        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack()
        );

        viewModel = new ViewModelProvider(requireActivity()).get(SessionsViewModel.class);

        if (viewModel.getSessions().getValue() == null ||
                viewModel.getSessions().getValue().isEmpty()) {
            viewModel.loadSessions();
        }

        viewModel.getSessions().observe(getViewLifecycleOwner(), this::updateAverages);

        return root;
    }

    private void updateAverages(List<WorkoutSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            setDisplay("--", 0, "--", 0, "--", 0);
            return;
        }

        long now = System.currentTimeMillis();
        long oneDayAgo = now - 1 * 24 * 60 * 60 * 1000L;
        long threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000L;
        long sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L;

        List<Integer> avg1 = new ArrayList<>();
        List<Integer> avg3 = new ArrayList<>();
        List<Integer> avg7 = new ArrayList<>();

        for (WorkoutSession s : sessions) {
            long endedAt = s.getEndedAt();
            int avgBpm = s.getAverageBpm();

            if (endedAt == 0 || avgBpm <= 0) continue; // skip incomplete sessions

            if (endedAt >= oneDayAgo) avg1.add(avgBpm);
            if (endedAt >= threeDaysAgo) avg3.add(avgBpm);
            if (endedAt >= sevenDaysAgo) avg7.add(avgBpm);
        }

        setDisplay(
                formatAverage(avg1), avg1.size(),
                formatAverage(avg3), avg3.size(),
                formatAverage(avg7), avg7.size()
        );
    }

    private String formatAverage(List<Integer> values) {
        if (values.isEmpty()) return "--";
        int sum = 0;
        for (int v : values) sum += v;
        return (sum / values.size()) + " bpm";
    }

    private void setDisplay(String avg1, int count1,
                            String avg3, int count3,
                            String avg7, int count7) {

        avg1DayView.setText(avg1);
        avg3DayView.setText(avg3);
        avg7DayView.setText(avg7);

        count1DayView.setText(count1 + " session" + (count1 == 1 ? "" : "s"));
        count3DayView.setText(count3 + " session" + (count3 == 1 ? "" : "s"));
        count7DayView.setText(count7 + " session" + (count7 == 1 ? "" : "s"));
    }
}
