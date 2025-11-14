package com.team11.smartgym.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.team11.smartgym.data.Reading;
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
                    List<Reading> readings = repo.getReadings(s.id);
                    int avg = readings.isEmpty() ? s.avgBpm : (int) readings.stream().mapToInt(r -> r.bpm).average().orElse(0);
                    int max = readings.isEmpty() ? s.maxBpm : readings.stream().mapToInt(r -> r.bpm).max().orElse(0);
                    int duration = (int) ((s.endedAt - s.startedAt) / 1000);

                    StringBuilder hrString = new StringBuilder();
                    for (Reading r : readings) hrString.append(r.bpm).append(",");
                    if (hrString.length() > 0) hrString.setLength(hrString.length() - 1);

                    list.add(new WorkoutSession(
                            s.id,
                            s.deviceName != null ? s.deviceName : "HR Sensor",
                            s.startedAt,
                            s.endedAt,
                            avg,
                            max,
                            duration,
                            hrString.toString()
                    ));
                }
            }
            return list;
        });
    }
}
