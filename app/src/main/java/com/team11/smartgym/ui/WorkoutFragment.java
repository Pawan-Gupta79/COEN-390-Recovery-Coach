package com.team11.smartgym.ui;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.team11.smartgym.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import com.google.android.material.snackbar.Snackbar;

public class WorkoutFragment extends Fragment {

    public static final String ARG_DEVICE_NAME = "arg_device_name";
    public static final String ARG_STARTED_AT = "arg_started_at";

    public static final int STATE_IDLE = -1;
    public static final int STATE_STOPPED = 0;
    public static final int STATE_STARTING = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_PAUSED = 3;


    private DashboardViewModel vm;
    private int state = STATE_IDLE;

    private TextView tvTimer, tvStatus;
    private MaterialButton btnPause, btnCancel, btnEnd;
    private final Handler handler = new Handler();

    private long startTime = 0L;
    private long pauseOffset = 0L;
    private long pauseStartTime = 0L;

    private static final int MAX_BPM_SAMPLES = 10;
    private static final long BPM_INTERVAL_MS = 1000;
    private long lastBpmUpdateTime = 0L;
    private final LinkedList<Integer> bpmSamples = new LinkedList<>();
    private final StringBuilder activityBpm = new StringBuilder();

    private int countdown = 3;
    private String selectedActivity = "Unknown";

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (state == STATE_STARTING) {
                if (countdown > 0) {
                    tvTimer.setText(String.valueOf(countdown));
                    countdown--;
                    handler.postDelayed(this, 1000);
                } else {
                    startMainTimer();
                }
            }
        }
    };

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (state == STATE_RUNNING) {
                long elapsed = System.currentTimeMillis() - startTime + pauseOffset;
                updateTimerDisplay(elapsed);
                handler.postDelayed(this, 10);

                vm.getBpm().observe(getViewLifecycleOwner(), bpm -> {
                    if (bpm != null && state == STATE_RUNNING) {
                        long now = System.currentTimeMillis();
                        if (now - lastBpmUpdateTime >= BPM_INTERVAL_MS) {
                            lastBpmUpdateTime = now;

                            bpmSamples.add(bpm);
                            if (bpmSamples.size() > MAX_BPM_SAMPLES)
                                bpmSamples.removeFirst();

                            double avg = bpmSamples.stream()
                                    .mapToInt(Integer::intValue)
                                    .average()
                                    .orElse(0);

                            activityBpm.append(String.format(Locale.getDefault(), "%.1f,", avg));
                        }
                    }
                });
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_workout, container, false);

        vm = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        TextView tvDevice = v.findViewById(R.id.tvWorkoutDevice);
        TextView tvStarted = v.findViewById(R.id.tvWorkoutStarted);
        tvTimer = v.findViewById(R.id.tvWorkoutTimer);
        btnPause = v.findViewById(R.id.btnPauseWorkout);
        btnCancel = v.findViewById(R.id.btnStopWorkout);
        btnEnd = v.findViewById(R.id.btnSaveWorkout);
        tvStatus = v.findViewById(R.id.tvWorkoutStatus);

        // Start in idle
        state = STATE_IDLE;
        tvStatus.setText("Idle");
        tvTimer.setText("00:00.00");
        btnPause.setText("Start Activity");
        btnPause.setEnabled(true);
        btnCancel.setEnabled(false);
        btnEnd.setEnabled(false);

        // single, stable click handler for the main button
        btnPause.setOnClickListener(v1 -> {
            if (state == STATE_IDLE) {
                showActivityChooser();
            } else {
                togglePauseResume();
            }
        });

        btnCancel.setOnClickListener(v12 -> confirmCancel());
        btnEnd.setOnClickListener(v13 -> confirmStop());

        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;
        String device = args.getString(ARG_DEVICE_NAME, "");
        long startedAt = args.getLong(ARG_STARTED_AT, 0L);

        String deviceLine = TextUtils.isEmpty(device)
                ? getString(R.string.workout_device_unknown)
                : getString(R.string.workout_device_fmt, device);

        String timeLine = startedAt == 0L
                ? ""
                : getString(R.string.workout_started_fmt,
                DateFormat.getTimeInstance(DateFormat.SHORT)
                        .format(new Date(startedAt)));

        tvDevice.setText(deviceLine);
        tvStarted.setText(timeLine);

        return v;
    }

    private void showActivityChooser() {
        String[] activities = {"Running", "Cycling", "Weightlifting", "Yoga", "Cardio"};

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Choose Activity")
                .setItems(activities, (dialog, which) -> {
                    selectedActivity = activities[which];
                    tvStatus.setText("Selected: " + selectedActivity);
                    startCountdown();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    state = STATE_IDLE;
                    tvStatus.setText("Idle");
                    tvTimer.setText("00:00.00");
                    btnPause.setText("Start Activity");
                })
                .show();
    }

    private void startCountdown() {
        state = STATE_STARTING;
        countdown = 3;
        tvTimer.setText(String.valueOf(countdown));
        tvStatus.setText("Starting " + selectedActivity);
        btnPause.setText("Pause Start");
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void startMainTimer() {
        state = STATE_RUNNING;
        tvStatus.setText(selectedActivity + " Ongoing");
        startTime = System.currentTimeMillis();
        pauseOffset = 0L;

        activityBpm.append(selectedActivity).append(",");
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

        tvTimer.setText("00:00.00");
        tvStatus.setText("Activity Ended");
        btnPause.setText("Start New Activity");
        btnPause.setEnabled(true);
        btnCancel.setEnabled(false);
        btnEnd.setEnabled(false);
        long totalElapsedSec = (System.currentTimeMillis() - startTime + pauseOffset) / 1000;
        if(save)
        {
            activityBpm.append("end").append(totalElapsedSec).append(",");
            Snackbar.make(requireView(), "Session saved", Snackbar.LENGTH_SHORT).show();
            System.out.println("BPM log: " + activityBpm);
            //Save to database functionality
        }
        activityBpm.setLength(0);
        state = STATE_IDLE;
    }

    private void confirmStop() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("End Activity")
                .setMessage("Do you want to save this activity?")
                .setPositiveButton("Save", (dialog, which) -> stopTimer(true))
                .setNegativeButton("Discard", (dialog, which) -> {
                    stopTimer(false);
                    activityBpm.setLength(0);
                })
                .setNeutralButton("Cancel", (dialog, which) -> {})
                .show();
    }

    private void confirmCancel() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cancel Workout")
                .setMessage("Are you sure you want to cancel this workout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    handler.removeCallbacks(timerRunnable);
                    handler.removeCallbacks(countdownRunnable);

                    state = STATE_IDLE;
                    tvStatus.setText("Idle");
                    tvTimer.setText("00:00.00");
                    btnPause.setText("Start Activity");
                    btnPause.setEnabled(true);
                    btnCancel.setEnabled(false);
                    btnEnd.setEnabled(false);

                    activityBpm.setLength(0);
                    pauseOffset = 0L;
                    startTime = 0L;
                    pauseStartTime = 0L;
                })
                .setNegativeButton("No", (dialog, which) -> {})
                .show();
    }

    private void updateTimerDisplay(long elapsedMillis) {
        int milliseconds = (int) (elapsedMillis % 1000) / 10;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        int hours = (int) (elapsedMillis / (1000 * 60 * 60));

        if (hours == 0 && minutes < 60) {
            tvTimer.setText(String.format(Locale.getDefault(),
                    "%02d:%02d.%02d", minutes, seconds, milliseconds));
        } else {
            tvTimer.setText(String.format(Locale.getDefault(),
                    "%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timerRunnable);
        handler.removeCallbacks(countdownRunnable);
    }
}
