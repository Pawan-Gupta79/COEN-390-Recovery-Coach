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

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import com.team11.smartgym.data.DatabaseProvider;

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
    private Button btnPause;
    private Button btnCancel;
    private Button btnEnd;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int state = STATE_IDLE;

    private long startTime      = 0L;
    private long pauseOffset    = 0L;
    private long pauseStartTime = 0L;

    private int countdown = 0;
    private static final int START_COUNTDOWN_SECONDS = 5;

    private String selectedActivity = "Workout";
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
        btnPause = v.findViewById(R.id.btnPause);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnEnd = v.findViewById(R.id.btnEnd);

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

        btnPause.setOnClickListener(view -> {
            if (state == STATE_IDLE) {
                startCountdown();
            } else {
                togglePauseResume();
            }
        });

        btnCancel.setOnClickListener(view -> confirmCancel());
        btnEnd.setOnClickListener(view -> confirmStop());

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

        return v;
    }

    private void loadWorkoutInfo() {
        Bundle args = getArguments();
        if (args != null) {
            String deviceName = args.getString(ARG_DEVICE_NAME, "");
            startedAtFromArgs = args.getLong(ARG_STARTED_AT, 0L);
            selectedActivity = (deviceName == null || deviceName.isEmpty())
                    ? "Workout"
                    : deviceName + " Workout";
        } else {
            selectedActivity = "Workout";
        }
        tvStatus.setText("Idle");
    }

    private void startCountdown() {
        state = STATE_STARTING;
        countdown = START_COUNTDOWN_SECONDS;
        tvTimer.setText(String.valueOf(countdown));
        tvStatus.setText("Starting " + selectedActivity);
        btnPause.setText("Pause Start");
        btnCancel.setEnabled(true);
        btnEnd.setEnabled(false);
        handler.postDelayed(countdownRunnable, 1000);
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
            btnCancel.setEnabled(true);

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

            if (dbProvider != null) {
                dbProvider.getDbExecutor().execute(() -> {
                try {
                    long sessionId = repo.createSession(finalStart);

                    for (Reading rr : toSave) {
                        rr.sessionId = sessionId;
                        repo.insertReading(rr);
                    }

                    repo.finalizeSession(sessionId, avgBpm, finalMax, finalEnd);

                    pendingReadings.clear();

                    handler.post(() -> Snackbar.make(requireView(),
                            "Saved: " + totalSec + " sec | Avg HR: " + avgBpm + " | Max HR: " + finalMax,
                            Snackbar.LENGTH_LONG).show());
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

        liveSessionId = -1;
        resetUI();
    }

    private void confirmStop() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("End Workout")
                .setMessage("Do you want to save this workout?")
                .setPositiveButton("Save", (dialog, which) -> stopTimer(true))
                .setNegativeButton("Discard", (dialog, which) -> stopTimer(false))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void confirmCancel() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cancel Activity")
                .setMessage("Are you sure you want to cancel this workout?")
                .setPositiveButton("Yes", (dialog, which) -> resetUI())
                .setNegativeButton("No", null)
                .show();
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
        outState.putString("Workout", selectedActivity);
        outState.putString("bpmData", activityBpm.toString());
    }

    private void restoreState(Bundle stateBundle) {
        state = stateBundle.getInt("state", STATE_IDLE);
        startTime = stateBundle.getLong("startTime", 0L);
        pauseOffset = stateBundle.getLong("pauseOffset", 0L);
        selectedActivity = stateBundle.getString("activity", "Workout");
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
                btnCancel.setEnabled(true);
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
