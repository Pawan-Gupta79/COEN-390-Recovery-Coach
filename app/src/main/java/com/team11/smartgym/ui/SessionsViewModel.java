package com.team11.smartgym.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.model.WorkoutSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ViewModel for SessionsFragment - manages workout session data. created by pawan
 */
public class SessionsViewModel extends ViewModel {

    private final MutableLiveData<List<WorkoutSession>> sessions = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<WorkoutSession>> getSessions() {
        return sessions;
    }

    /**
     * Load sessions - for now, generate mock data.
     * In a real app, this would fetch from a repository/database.
     */
    public void loadSessions() {
        List<WorkoutSession> mockSessions = generateMockSessions();

        // Sort in reverse chronological order (newest first)
        Collections.sort(mockSessions, new Comparator<WorkoutSession>() {
            @Override
            public int compare(WorkoutSession s1, WorkoutSession s2) {
                return Long.compare(s2.getStartedAt(), s1.getStartedAt());
            }
        });

        sessions.setValue(mockSessions);
    }

    /**
     * Generate mock workout sessions for demonstration.
     * Replace this with actual database queries later.
     */
    private List<WorkoutSession> generateMockSessions() {
        List<WorkoutSession> list = new ArrayList<>();

        long now = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60 * 1000L;

        // Add some sample sessions from the past week
        list.add(new WorkoutSession(
                1,
                "SmartGym HR-1A",
                now - oneDay,
                now - oneDay + (45 * 60 * 1000),
                128,
                165,
                45 * 60
        ));

        list.add(new WorkoutSession(
                2,
                "Polar H10",
                now - (2 * oneDay),
                now - (2 * oneDay) + (30 * 60 * 1000),
                115,
                142,
                30 * 60
        ));

        list.add(new WorkoutSession(
                3,
                "SmartGym HR-1A",
                now - (4 * oneDay),
                now - (4 * oneDay) + (60 * 60 * 1000),
                135,
                178,
                60 * 60
        ));

        list.add(new WorkoutSession(
                4,
                "Garmin HRM",
                now - (5 * oneDay),
                now - (5 * oneDay) + (25 * 60 * 1000),
                110,
                138,
                25 * 60
        ));

        list.add(new WorkoutSession(
                5,
                "SmartGym HR-2B",
                now - (7 * oneDay),
                now - (7 * oneDay) + (40 * 60 * 1000),
                122,
                155,
                40 * 60
        ));

        return list;
    }

    /**
     * Add a new session (for future use when completing workouts).
     */
    public void addSession(WorkoutSession session) {
        List<WorkoutSession> current = sessions.getValue();
        if (current != null) {
            List<WorkoutSession> updated = new ArrayList<>(current);
            updated.add(0, session); // Add to beginning (most recent)
            sessions.setValue(updated);
        }
    }

    /**
     * Clear all sessions (for testing or reset functionality).
     */
    public void clearSessions() {
        sessions.setValue(new ArrayList<>());
    }
}