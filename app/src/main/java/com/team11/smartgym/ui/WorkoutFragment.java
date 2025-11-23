package com.team11.smartgym.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
// DatabaseProvider already imported above
import com.team11.smartgym.data.Reading;
import com.team11.smartgym.data.SessionRepository;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import com.team11.smartgym.data.DatabaseProvider;
import androidx.navigation.fragment.NavHostFragment;

public class WorkoutFragment extends Fragment {

    public static final String ARG_DEVICE_NAME = "arg_device_name";
    public static final String ARG_STARTED_AT  = "arg_started_at";

    private static final int STATE_IDLE      = 0;
    private static final int STATE_STARTING  = 1;
    private static final int STATE_RUNNING   = 2;
    private static final int STATE_PAUSED    = 3;

    private TextView tvTimer;
    private TextView tvStatus;
    private TextView tvBpm;
    private TextView tvAvgBpm;
    private TextView tvMaxBpm;
    private TextView tvWorkoutStarted;
    private Button btnPause;
    private Button btnCancel;
    private Button btnEnd;
    private androidx.recyclerview.widget.RecyclerView rvWorkoutSessions;
    private SessionsAdapter sessionsAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int state = STATE_IDLE;

    private long startTime      = 0L;
    private long pauseOffset    = 0L;
    private long pauseStartTime = 0L;

    private int countdown = 0;
    private static final int START_COUNTDOWN_SECONDS = 5;

    private String selectedActivity = "Other";
    private long startedAtFromArgs = 0L;
    private final StringBuilder activityBpm = new StringBuilder();

    private DashboardViewModel vm;
    private final Handler bpmHandler = new Handler(Looper.getMainLooper());
    private static final int BPM_UPDATE_INTERVAL = 1000;

    // BPM tracking
    private int maxBpm = 0;
    private int bpmSum = 0;
    private int bpmCount = 0;

    // Use DatabaseProvider to get repository (fixed)
    private SessionRepository repo;
    private long liveSessionId = -1;
    private final List<Reading> pendingReadings = new ArrayList<>();
    private DatabaseProvider dbProvider;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedMillis = System.currentTimeMillis() - startTime + pauseOffset;
            updateTimerDisplay(elapsedMillis);
            handler.postDelayed(this, 10);
        }
    };
    

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (countdown > 0) {
                countdown--;
                tvTimer.setText(String.valueOf(countdown));
                tvStatus.setText("Starting " + selectedActivity);
                btnPause.setText("Pause Start");
                handler.postDelayed(this, 1000);
            } else {
                startMainTimer();
            }
        }
    };
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_workout, container, false);

        tvTimer  = v.findViewById(R.id.tvTimer);
        tvStatus = v.findViewById(R.id.tvStatus);
        tvBpm    = v.findViewById(R.id.tvBpm);
        tvAvgBpm = v.findViewById(R.id.tvAvgBpm);
        tvMaxBpm = v.findViewById(R.id.tvMaxBpm);
        tvWorkoutStarted = v.findViewById(R.id.tvWorkoutStarted);
        btnPause = v.findViewById(R.id.btnPause);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnEnd = v.findViewById(R.id.btnEnd);
        btnCancel.setText(R.string.end_workout);

        vm = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        // ===== FIX: obtain SessionRepository and shared DB executor from DatabaseProvider =====
        try {
            dbProvider = DatabaseProvider.get(requireContext());
            repo = dbProvider.getSessionRepository();
        } catch (Exception e) {
            dbProvider = null;
            repo = null;
            Snackbar.make(v, "Database unavailable: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }

        startBpmUpdater();
        loadWorkoutInfo();
        resetUI();

        // If a workout already exists (controller has current workout id) and we're idle,
        // allow the user to End the workout (save/discard).
        try {
            if (dbProvider != null && dbProvider.getSessionController().getCurrentWorkoutId() != null) {
                btnCancel.setEnabled(true);
            }
        } catch (Exception ignored) {}

        // setup sessions list (hidden by default)
        rvWorkoutSessions = v.findViewById(R.id.rvWorkoutSessions);
        sessionsAdapter = new SessionsAdapter();
        rvWorkoutSessions.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        rvWorkoutSessions.setAdapter(sessionsAdapter);
        rvWorkoutSessions.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(requireContext(), androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));

        btnPause.setOnClickListener(view -> {
            if (state == STATE_IDLE) {
                showActivityChooserAndStart();
            } else {
                togglePauseResume();
            }
        });

        // btnCancel (End Workout) opens the workout-level dialog; btnEnd ends the current activity only
        btnCancel.setOnClickListener(view -> confirmEndWorkout());
        btnEnd.setOnClickListener(view -> confirmEndActivity());

        // Intercept back presses: if a workout exists or session active, prompt save/discard; otherwise allow back
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                boolean hasWorkout = false;
                try {
                    if (dbProvider != null && dbProvider.getSessionController().getCurrentWorkoutId() != null) hasWorkout = true;
                } catch (Exception ignored) {}

                if (state != STATE_IDLE) {
                    // There is an active activity/session: block back navigation.
                    // Ask the user to finish the activity first instead of prompting a dialog.
                    try {
                        Snackbar.make(requireView(), "Please end the activity before leaving.", Snackbar.LENGTH_SHORT).show();
                    } catch (Exception ignored) {}
                    return;
                } else if (hasWorkout) {
                    // no active session but a workout exists: prompt to end/keep or discard workout
                    confirmEndWorkout();
                } else {
                    // allow normal back
                    setEnabled(false);
                    requireActivity().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

        return v;
    }

    private void loadWorkoutInfo() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("workoutId")) {
            // View-only mode for an existing workout
            long wid = args.getLong("workoutId", -1L);
            if (wid > 0 && repo != null) {
                // disable controls
                btnPause.setEnabled(false);
                btnCancel.setEnabled(false);
                btnEnd.setEnabled(false);

                // observe workout meta
                repo.getWorkoutLiveById(wid).observe(getViewLifecycleOwner(), workout -> {
                    if (workout != null) {
                            // Workout is a group; activity types are stored per-session.
                            tvStatus.setText("Completed");
                            tvBpm.setText("-- bpm");
                            tvAvgBpm.setText("Average: " + workout.avgBpm + " bpm");
                            tvMaxBpm.setText("Max: " + workout.maxBpm + " bpm");
                            java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
                            tvWorkoutStarted.setText(df.format(new java.util.Date(workout.startedAt)));
                        }
                });

                // observe sessions and populate the recycler view
                repo.getSessionsForWorkoutLive(wid).observe(getViewLifecycleOwner(), sessions -> {
                    if (sessions == null || sessions.isEmpty()) {
                        rvWorkoutSessions.setVisibility(View.GONE);
                        return;
                    }
                    // map DB Session -> UI WorkoutSession model
                    java.util.List<com.team11.smartgym.model.WorkoutSession> list = new java.util.ArrayList<>();
                    for (com.team11.smartgym.data.Session s : sessions) {
                        int duration = (int) ((s.endedAt - s.startedAt) / 1000);
                        // use the session's activity type
                        String deviceName = (s.type == null || s.type.isEmpty()) ? "Other" : s.type;
                        com.team11.smartgym.model.WorkoutSession ws = new com.team11.smartgym.model.WorkoutSession(
                            s.id,
                            deviceName,
                            s.startedAt,
                            s.endedAt,
                            s.avgBpm,
                            s.maxBpm,
                            duration
                        );
                        list.add(ws);
                    }
                    sessionsAdapter.submitList(list);
                    rvWorkoutSessions.setVisibility(View.VISIBLE);
                });
            }
        } else {
            String deviceName = args == null ? "" : args.getString(ARG_DEVICE_NAME, "");
            startedAtFromArgs = args == null ? 0L : args.getLong(ARG_STARTED_AT, 0L);
            selectedActivity = (deviceName == null || deviceName.isEmpty())
                    ? "Other"
                    : deviceName + " Workout";
            tvStatus.setText("Idle");

            // no workout-level activity type; sessions carry activity types
        }
    }

    private void startCountdown() {
        state = STATE_STARTING;
        countdown = START_COUNTDOWN_SECONDS;
        tvTimer.setText(String.valueOf(countdown));
        tvStatus.setText("Starting " + selectedActivity);
        btnPause.setText("Pause Start");
        // End Workout should not be available while starting
        btnCancel.setEnabled(false);
        btnEnd.setEnabled(false);
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void showActivityChooserAndStart() {
        final String[] items = new String[]{"Run", "Jog", "Cycling", "Yoga", "Weightlifting", "Other"};
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setTitle("Choose activity");
        b.setItems(items, (dialog, which) -> {
            selectedActivity = items[which];
            tvStatus.setText("Selected: " + selectedActivity);
            // persist activity type on workout row if one exists
            if (dbProvider != null) {
                try {
                    Long wid = null;
                    try { wid = dbProvider.getSessionController().getCurrentWorkoutId(); } catch (Exception ignored) {}
                    if (wid == null) {
                        // no workout exists yet: create one synchronously so we can persist the type
                        try {
                            long newWid = dbProvider.getSessionController().startWorkoutSync(System.currentTimeMillis());
                            if (newWid > 0) wid = newWid;
                        } catch (Exception ignored) {}
                    }

                    // no longer persisting activity type on the Workout; type is stored on each Session when saved
                } catch (Exception ignored) {}
            }
            startCountdown();
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void startMainTimer() {
        state = STATE_RUNNING;
        tvStatus.setText(selectedActivity + " Ongoing");

        startTime = System.currentTimeMillis();
        pauseOffset = 0L;

        activityBpm.setLength(0);
        activityBpm.append(selectedActivity).append(",");

        maxBpm = 0;
        bpmSum = 0;
        bpmCount = 0;

        // Do not create DB session here. We'll persist when user chooses to save.
        liveSessionId = -1;
        pendingReadings.clear();

        handler.removeCallbacks(countdownRunnable);
        handler.post(timerRunnable);

        btnPause.setEnabled(true);
        btnPause.setText("Pause Workout");
        btnCancel.setEnabled(false);
        btnEnd.setEnabled(false);
    }

    private void togglePauseResume() {
        if (state == STATE_STARTING) {
            state = STATE_PAUSED;
            tvStatus.setText("Starting Paused");
            handler.removeCallbacks(countdownRunnable);
            btnPause.setText("Resume Start");
        } else if (state == STATE_PAUSED && countdown > 0) {
            state = STATE_STARTING;
            tvStatus.setText("Starting " + selectedActivity);
            btnPause.setText("Pause Start");
            handler.postDelayed(countdownRunnable, 1000);
        } else if (state == STATE_RUNNING) {
            state = STATE_PAUSED;
            pauseStartTime = System.currentTimeMillis();
            pauseOffset += pauseStartTime - startTime;
            handler.removeCallbacks(timerRunnable);

            btnPause.setText("Resume Workout");
            tvStatus.setText(selectedActivity + " Paused");
            btnEnd.setEnabled(true);
            // End Workout only allowed when there's no ongoing activity (not while paused)
            btnCancel.setEnabled(false);

            long elapsedSec = pauseOffset / 1000;
            activityBpm.append("pause").append(elapsedSec).append(",");
        } else if (state == STATE_PAUSED && countdown == 0) {
            state = STATE_RUNNING;
            startTime = System.currentTimeMillis();
            handler.post(timerRunnable);

                btnPause.setText("Pause Workout");
                tvStatus.setText(selectedActivity + " Ongoing");
                btnEnd.setEnabled(false);
                btnCancel.setEnabled(false);
        }
    }

    private void stopTimer(boolean save) {
        handler.removeCallbacks(timerRunnable);
        handler.removeCallbacks(countdownRunnable);

        long endTime = System.currentTimeMillis();
        long totalElapsedSec = (endTime - startTime + pauseOffset) / 1000;
        if (save && repo != null) {
            final int avgBpm = bpmCount == 0 ? 0 : bpmSum / bpmCount;
            final int finalMax = maxBpm;
            final long finalStart = startTime;
            final long finalEnd = endTime;
            final int totalSec = (int) totalElapsedSec;
            final List<Reading> toSave = new ArrayList<>(pendingReadings);
            final String finalSelectedActivity = selectedActivity;

            if (dbProvider != null) {
                dbProvider.getDbExecutor().execute(() -> {
                    try {
                        Long wid = null;
                        try {
                            wid = dbProvider.getSessionController().getCurrentWorkoutId();
                        } catch (Exception ignored) {}

                        long sessionId = repo.createSession(finalStart, wid, finalSelectedActivity);

                        for (Reading rr : toSave) {
                            rr.sessionId = sessionId;
                            repo.insertReading(rr);
                        }

                        repo.finalizeSession(sessionId, avgBpm, finalMax, finalEnd);

                        // Recompute workout summary so the Workout row shows up-to-date avg/max/duration.
                        try {
                            if (dbProvider != null && wid != null) {
                                // we're already running on the DB executor, so call the sync variant
                                dbProvider.getSessionController().recomputeWorkoutSummarySync(wid);
                            }
                        } catch (Exception ignored) {}

                        // Keep the workout active so the user can start another session within it.
                        pendingReadings.clear();

                        handler.post(() -> {
                            Snackbar.make(requireView(),
                                    "Saved: " + totalSec + " sec | Avg HR: " + avgBpm + " | Max HR: " + finalMax,
                                    Snackbar.LENGTH_LONG).show();
                            try {
                                // If a workout is active, allow starting a new session (re-enable start button).
                                boolean workoutActive = false;
                                try {
                                    if (dbProvider != null && dbProvider.getSessionController().getCurrentWorkoutId() != null) {
                                        workoutActive = true;
                                    }
                                } catch (Exception ignored) {}

                                if (workoutActive) {
                                    tvStatus.setText("Saved");
                                    btnPause.setEnabled(true);
                                    btnPause.setText("Start Activity");
                                    btnEnd.setEnabled(false);
                                    // enable End Workout only when idle and workout exists
                                    btnCancel.setEnabled(true);
                                } else {
                                    // no active workout: show read-only saved state
                                    tvStatus.setText("Completed");
                                    btnPause.setEnabled(false);
                                    btnPause.setText("Start Activity");
                                    btnEnd.setEnabled(false);
                                    btnCancel.setEnabled(false);
                                }

                                // ensure sessions list is visible when saved
                                if (rvWorkoutSessions != null) rvWorkoutSessions.setVisibility(View.VISIBLE);
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception e) {
                        handler.post(() -> Snackbar.make(requireView(), "Failed to save workout: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
                    }
                });
            } else {
                handler.post(() -> Snackbar.make(requireView(), "Failed to save workout: database unavailable", Snackbar.LENGTH_LONG).show());
            }
        } else if (save && repo == null) {
            Snackbar.make(requireView(), "Can't save: database unavailable", Snackbar.LENGTH_LONG).show();
        }


        // When stopping an activity (save==false) we discard only the current session's in-memory readings
        // and do NOT delete the enclosing workout. Workout deletion is handled by the End Workout flow.
        liveSessionId = -1;
        if (!save) {
            // If a workout still exists, keep the workout active and allow the user to End Workout.
            boolean workoutActive = false;
            try {
                if (dbProvider != null && dbProvider.getSessionController().getCurrentWorkoutId() != null) {
                    workoutActive = true;
                }
            } catch (Exception ignored) {}

            if (workoutActive) {
                handler.post(() -> {
                    try {
                        // update state to idle (activity ended) but keep workout active
                        state = STATE_IDLE;
                        // stop outstanding runnables and reset timer display
                        handler.removeCallbacks(timerRunnable);
                        handler.removeCallbacks(countdownRunnable);
                        tvTimer.setText("00:00.00");
                        pauseOffset = 0L;
                        startTime = 0L;
                        pauseStartTime = 0L;
                        activityBpm.setLength(0);
                        maxBpm = 0;
                        bpmSum = 0;
                        bpmCount = 0;
                        if (tvAvgBpm != null) tvAvgBpm.setText("Average: --");
                        if (tvMaxBpm != null) tvMaxBpm.setText("Max: --");

                        tvStatus.setText("Saved");
                        btnPause.setEnabled(true);
                        btnPause.setText("Start Activity");
                        btnEnd.setEnabled(false);
                        btnCancel.setEnabled(true); // enable End Workout
                        if (rvWorkoutSessions != null) rvWorkoutSessions.setVisibility(View.VISIBLE);
                    } catch (Exception ignored) {}
                });
            } else {
                // no active workout: full reset
                resetUI();
            }
        } else {
            resetUI();
        }
    }

    private void confirmEndActivity() {
        // Prevent saving an empty activity (no readings collected)
        if (pendingReadings == null || pendingReadings.isEmpty() || bpmCount == 0) {
            try {
                Snackbar.make(requireView(), "No activity data to save", Snackbar.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("End Activity")
                .setMessage("Do you want to save this activity?")
                .setPositiveButton("Save", (dialog, which) -> stopTimer(true))
                .setNegativeButton("Discard", (dialog, which) -> stopTimer(false))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void confirmEndWorkout() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("End Workout")
                .setMessage("Do you want to finalize and keep this workout, or discard the entire workout and its sessions?")
                .setPositiveButton("Finalize", (dialog, which) -> endWorkoutFlow(true))
                .setNegativeButton("Discard Workout", (dialog, which) -> endWorkoutFlow(false))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void endWorkoutFlow(boolean finalize) {
        if (dbProvider == null || repo == null) {
            handler.post(() -> Snackbar.make(requireView(), "Database unavailable", Snackbar.LENGTH_SHORT).show());
            return;
        }
        dbProvider.getDbExecutor().execute(() -> {
            try {
                Long wid = null;
                try { wid = dbProvider.getSessionController().getCurrentWorkoutId(); } catch (Exception ignored) {}
                if (wid == null) {
                    handler.post(() -> Snackbar.make(requireView(), "No active workout to end.", Snackbar.LENGTH_SHORT).show());
                    return;
                }

                if (finalize) {
                    // Compute and persist workout summary and clear currentWorkoutId
                    try { dbProvider.getSessionController().endWorkout(System.currentTimeMillis()); } catch (Exception ignored) {}
                    handler.post(() -> {
                        try { Snackbar.make(requireView(), "Workout finalized", Snackbar.LENGTH_SHORT).show(); } catch (Exception ignored) {}
                        try { NavHostFragment.findNavController(WorkoutFragment.this).popBackStack(); } catch (Exception ignored) {}
                    });
                } else {
                    // Discard entire workout (sessions + readings + workout row)
                    try { repo.deleteWorkoutCascade(wid); } catch (Exception ignored) {}
                    try { dbProvider.getSessionController().cancelWorkout(wid); } catch (Exception ignored) {}
                    handler.post(() -> {
                        try { Snackbar.make(requireView(), "Workout discarded", Snackbar.LENGTH_SHORT).show(); } catch (Exception ignored) {}
                        try { NavHostFragment.findNavController(WorkoutFragment.this).popBackStack(); } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    private void resetUI() {
        state = STATE_IDLE;
        tvStatus.setText("Idle");
        tvTimer.setText("00:00.00");
        btnPause.setText("Start Activity");
        btnPause.setEnabled(true);
        btnCancel.setEnabled(false);
        btnEnd.setEnabled(false);

        pauseOffset = 0L;
        startTime = 0L;
        pauseStartTime = 0L;
        activityBpm.setLength(0);

        maxBpm = 0;
        bpmSum = 0;
        bpmCount = 0;

        if (tvAvgBpm != null) tvAvgBpm.setText("Average: --");
        if (tvMaxBpm != null) tvMaxBpm.setText("Max: --");
    }

    private void updateTimerDisplay(long elapsedMillis) {
        int milliseconds = (int) (elapsedMillis % 1000) / 10;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        int hours = (int) (elapsedMillis / (1000 * 60 * 60));

        if (hours == 0 && minutes < 60) {
            tvTimer.setText(String.format(
                    Locale.getDefault(),
                    "%02d:%02d.%02d", minutes, seconds, milliseconds
            ));
        } else {
            tvTimer.setText(String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d", hours, minutes, seconds
            ));
        }
    }

    private void startBpmUpdater() {
        bpmHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Integer bpm = null;
                try {
                    bpm = vm.getBpm().getValue(); // keep original approach
                } catch (Exception e) {
                    // guard against ViewModel issues
                }

                if (tvBpm != null && isAdded()) {
                    tvBpm.setText(bpm == null ? "-- bpm" : getString(R.string.hr_bpm, bpm));
                }

                if (bpm != null && state == STATE_RUNNING) {

                    bpmSum += bpm;
                    bpmCount++;

                    if (bpm > maxBpm) maxBpm = bpm;

                    // collect reading in-memory; persist on save
                    Reading r = new Reading();
                    r.sessionId = -1; // assigned when saved
                    r.timestamp = System.currentTimeMillis();
                    r.bpm = bpm;
                    pendingReadings.add(r);
                }

                if (tvAvgBpm != null) {
                    int avg = (bpmCount == 0 ? 0 : bpmSum / bpmCount);
                    tvAvgBpm.setText("Average: " + avg + " bpm");
                }

                if (tvMaxBpm != null) {
                    tvMaxBpm.setText("Max: " + maxBpm + " bpm");
                }

                bpmHandler.postDelayed(this, BPM_UPDATE_INTERVAL);
            }
        }, BPM_UPDATE_INTERVAL);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("state", state);
        outState.putLong("startTime", startTime);
        outState.putLong("pauseOffset", pauseOffset);
        outState.putString("activity", selectedActivity);
        outState.putString("bpmData", activityBpm.toString());
    }

    private void restoreState(Bundle stateBundle) {
        state = stateBundle.getInt("state", STATE_IDLE);
        startTime = stateBundle.getLong("startTime", 0L);
        pauseOffset = stateBundle.getLong("pauseOffset", 0L);
        selectedActivity = stateBundle.getString("activity", "Other");
        activityBpm.setLength(0);
        activityBpm.append(stateBundle.getString("bpmData", ""));
        updateUIState();
    }

    private void updateUIState() {
        switch (state) {
            case STATE_RUNNING:
                tvStatus.setText(selectedActivity + " Ongoing");
                btnPause.setText("Pause Activity");
                btnCancel.setEnabled(false);
                btnEnd.setEnabled(false);
                handler.post(timerRunnable);
                break;
            case STATE_PAUSED:
                tvStatus.setText(selectedActivity + " Paused");
                btnPause.setText("Resume Activity");
                // Do not allow ending the workout while a session is paused
                btnCancel.setEnabled(false);
                btnEnd.setEnabled(true);
                break;
            default:
                resetUI();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timerRunnable);
        handler.removeCallbacks(countdownRunnable);
        bpmHandler.removeCallbacksAndMessages(null);
        // shared executor is owned by DatabaseProvider; do not shut it down here
    }
}
