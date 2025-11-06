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

import com.google.android.material.button.MaterialButton;
import com.team11.smartgym.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkoutFragment extends Fragment {

    public static final String ARG_DEVICE_NAME = "arg_device_name";
    public static final String ARG_STARTED_AT  = "arg_started_at";

    // Integer state constants
    public static final int STATE_STOPPED = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSED  = 2;

    private int state = STATE_STOPPED;

    private TextView tvTimer, tvStatus;;
    private MaterialButton btnPause, btnStop, btnSave;
    private final Handler handler = new Handler();

    private long startTime = 0L;
    private long pauseOffset = 0L;

    // Runnable timer loop
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (state == STATE_RUNNING) {
                long elapsed = System.currentTimeMillis() - startTime + pauseOffset;
                updateTimerDisplay(elapsed);
                handler.postDelayed(this, 10);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_workout, container, false);

        TextView tvDevice  = v.findViewById(R.id.tvWorkoutDevice);
        TextView tvStarted = v.findViewById(R.id.tvWorkoutStarted);
        tvTimer = v.findViewById(R.id.tvWorkoutTimer);
        btnPause = v.findViewById(R.id.btnPauseWorkout);
        btnStop = v.findViewById(R.id.btnStopWorkout);
        btnSave = v.findViewById(R.id.btnSaveWorkout);
        tvStatus = v.findViewById(R.id.tvWorkoutStatus);
        tvStatus.setText("Activity Ongoing");

        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;
        String device   = args.getString(ARG_DEVICE_NAME, "");
        long startedAt  = args.getLong(ARG_STARTED_AT, 0L);

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

        startTimer();

        btnPause.setOnClickListener(v1 -> togglePauseResume());
        btnStop.setOnClickListener(v12 -> stopTimer());

        return v;
    }

    private void startTimer() {
        if (state == STATE_STOPPED) {
            state = STATE_RUNNING;
            startTime = System.currentTimeMillis();
            pauseOffset = 0L;
            handler.post(timerRunnable);
            btnPause.setEnabled(true);
            btnStop.setEnabled(true);
            btnSave.setEnabled(false);
        }
    }

    private void togglePauseResume() {
        if (state == STATE_RUNNING) {
            state = STATE_PAUSED;
            pauseOffset += System.currentTimeMillis() - startTime;
            handler.removeCallbacks(timerRunnable);
            btnPause.setText("Resume Workout");
            tvTimer.setAlpha(0.5f);
            tvStatus.setText("Activity Paused");
        } else if (state == STATE_PAUSED) {
            state = STATE_RUNNING;
            startTime = System.currentTimeMillis();
            handler.post(timerRunnable);
            btnPause.setText("Pause Workout");
            tvTimer.setAlpha(1.0f);
            tvStatus.setText("Activity Ongoing");
        }
    }

    private void stopTimer() {
        state = STATE_STOPPED;
        handler.removeCallbacks(timerRunnable);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        btnSave.setEnabled(true);
    }

    private void updateTimerDisplay(long elapsedMillis) {
        int milliseconds = (int) (elapsedMillis % 1000) / 10; // hundredths
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        int hours   = (int) (elapsedMillis / (1000 * 60 * 60));

        // Before 60 minutes → show mm:ss.ms
        if (hours == 0 && minutes < 60) {
            tvTimer.setText(String.format(Locale.getDefault(),
                    "%02d:%02d.%02d", minutes, seconds, milliseconds));
        } else {
            // After 60 minutes → show hh:mm:ss
            tvTimer.setText(String.format(Locale.getDefault(),
                    "%02d:%02d:%02d", hours, minutes, seconds));
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timerRunnable);
    }
}
