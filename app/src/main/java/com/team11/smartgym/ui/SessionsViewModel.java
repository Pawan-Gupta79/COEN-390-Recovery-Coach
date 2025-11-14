package com.team11.smartgym.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.data.Session;
import com.team11.smartgym.data.SessionRepository;
import com.team11.smartgym.model.WorkoutSession;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for SessionsFragment
 */
public class SessionsViewModel extends ViewModel {

    private final SessionRepository repo;
    private final LiveData<List<Session>> allSessions;

    public SessionsViewModel(SessionRepository repository) {
        this.repo = repository;
        this.allSessions = repo.getAllSessions();
    }

    /**
     * Expose sessions as LiveData of WorkoutSession for the adapter
     */
    public LiveData<List<WorkoutSession>> getSessions() {
        return Transformations.map(allSessions, sessions -> {
            List<WorkoutSession> list = new ArrayList<>();
            if (sessions != null) {
                for (Session s : sessions) {
                    // Avoid synchronous DB reads on the main thread. Use stored summary fields.
                    int avg = s.avgBpm;
                    int max = s.maxBpm;
                    int duration = (int) ((s.endedAt - s.startedAt) / 1000);

                    list.add(new WorkoutSession(
                            s.id,
                            s.deviceName != null ? s.deviceName : "HR Sensor",
                            s.startedAt,
                            s.endedAt,
                            avg,
                            max,
                            duration,
                            "" // detailed HR string omitted to avoid extra DB access
                    ));
                }
            }
            return list;
        });
    }
}
