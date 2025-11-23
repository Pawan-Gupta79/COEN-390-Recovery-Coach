package com.team11.smartgym.ui.transform;

import com.team11.smartgym.data.Session;
import com.team11.smartgym.model.WorkoutSession;

import java.util.ArrayList;
import java.util.List;

public class SessionTransform {
    public static List<WorkoutSession> toWorkoutSessionList(List<Session> sessions) {
        if (sessions == null) return null;
        List<WorkoutSession> out = new ArrayList<>();
        for (Session s : sessions) {
            int duration = (int) ((s.endedAt - s.startedAt) / 1000);
            String type = (s.type == null || s.type.isEmpty()) ? "Other" : s.type;
            out.add(new WorkoutSession(s.id, type, s.startedAt, s.endedAt, s.avgBpm, s.maxBpm, duration));
        }
        return out;
    }
}
