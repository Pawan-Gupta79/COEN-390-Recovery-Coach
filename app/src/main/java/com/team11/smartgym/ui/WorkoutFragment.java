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

import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
import com.team11.smartgym.model.WorkoutSession; // Added import

import java.util.Locale;

public class WorkoutFragment extends Fragment {

    // Args used by DashboardFragment when navigating here
    public static final String ARG_DEVICE_NAME = "arg_device_name";
    public static final String ARG_STARTED_AT  = "arg_started_at";

    // ---- State constants ----
    private static final int STATE_IDLE      = 0;
    private static final int STATE_STARTING  = 1;
    private static final int STATE_RUNNING   = 2;
    private static final int STATE_PAUSED    = 3;

    // ---- UI ----
    private TextView tvTimer;
    private TextView tvStatus;
    private Button btnPause;
    private Button btnCancel;
    private Button btnEnd;

    // ---- Timer & state ----
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int state = STATE_IDLE;

    private long startTime      = 0L;
    private long pauseOffset    = 0L;
    private long pauseStartTime = 0L;

    private int countdown = 0;
    private static final int START_COUNTDOWN_SECONDS = 5;

    private String selectedActivity = "Workout";
    private long startedAtFromArgs = 0L; // from Dashboard
    private final StringBuilder activityBpm = new StringBuilder();

    // ---- Runnables ----
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

    // ---- Fragment lifecycle ----

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_workout, container, false);

        tvTimer  = v.findViewById(R.id.tvTimer);
        tvStatus = v.findViewById(R.id.tvStatus);
        btnPause = v.findViewById(R.id.btnPause);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnEnd = v.findViewById(R.id.btnEnd);

        // Load info passed from DashboardFragment
        loadWorkoutInfo();

        resetUI();

        btnPause.setOnClickListener(view -> {
            if (state == STATE_IDLE) {
                // start countdown to workout
                startCountdown();
            } else {
                // toggle pause / resume logic
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

    /**
     * Load information about the workout (e.g., device name and start time) from arguments.
     * This matches what DashboardFragment puts into the Bundle.
     */
    private void loadWorkoutInfo() {
        Bundle args = getArguments();
        if (args != null) {
            String deviceName = args.getString(ARG_DEVICE_NAME, "");
            startedAtFromArgs = args.getLong(ARG_STARTED_AT, 0L);

            // Use device name as part of the activity label (fallback to "Workout")
            selectedActivity = (deviceName == null || deviceName.isEmpty())
                    ? "Workout"
                    : deviceName + " Workout";
        } else {
            selectedActivity = "Workout";
        }
        tvStatus.setText("Idle");
    }

    // ---- State control methods ----

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

        if (save) {
            // Create a WorkoutSession instance to store the workout details
            WorkoutSession session = new WorkoutSession(
                    System.currentTimeMillis(),                              // id
                    selectedActivity,                                        // deviceName
                    startedAtFromArgs == 0L ? startTime : startedAtFromArgs, // startedAt
                    endTime,                                                 // endedAt
                    0,                                                       // avgHeartRate (placeholder)  //TODO CHANGE THIS TO THE RIGHT THING
                    0,                                                       // maxHeartRate (placeholder)
                    (int) totalElapsedSec,                                   // duration in seconds
                    activityBpm.toString()                                   // saves a string of heartrate "bpm1, bpm2, bpm3, ..."
            );

            Snackbar.make(requireView(),
                    "Session saved (" + session.getFormattedDuration() + ")",
                    Snackbar.LENGTH_SHORT).show();
        }

        resetUI();
    }

    private void confirmStop() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("End Activity")
                .setMessage("Do you want to save this activity?")
                .setPositiveButton("Save", (dialog, which) -> stopTimer(true))
                .setNegativeButton("Discard", (dialog, which) -> stopTimer(false))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void confirmCancel() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cancel Workout")
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
        selectedActivity = stateBundle.getString("activity", "Workout");
        activityBpm.setLength(0);
        activityBpm.append(stateBundle.getString("bpmData", ""));
        updateUIState();
    }

    private void updateUIState() {
        switch (state) {
            case STATE_RUNNING:
                tvStatus.setText(selectedActivity + " Ongoing");
                btnPause.setText("Pause Workout");
                btnCancel.setEnabled(false);
                btnEnd.setEnabled(false);
                handler.post(timerRunnable);
                break;
            case STATE_PAUSED:
                tvStatus.setText(selectedActivity + " Paused");
                btnPause.setText("Resume Workout");
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
    }
}
