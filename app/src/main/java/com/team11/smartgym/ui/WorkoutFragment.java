package com.team11.smartgym.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.team11.smartgym.R;

import java.text.DateFormat;
import java.util.Date;

/**
 * Fragment to display workout session information.
 * This is typically shown during an active workout session.
 */
public class INWorkoutFragment extends Fragment {

    public static final String ARG_DEVICE_NAME = "arg_device_name";
    public static final String ARG_STARTED_AT  = "arg_started_at";

    public String timeLine;

    private TextView tvWorkoutDevice;
    private TextView tvWorkoutStarted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_workout, container, false);

        tvWorkoutDevice = v.findViewById(R.id.tvWorkoutDevice);
        tvWorkoutStarted = v.findViewById(R.id.tvWorkoutStarted);

        // Load arguments
        loadWorkoutInfo();

        return v;
    }

    /**
     * Load workout information from arguments and display
     */
    private void loadWorkoutInfo() {
        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;
        String device = args.getString(ARG_DEVICE_NAME, "");
        long startedAt = args.getLong(ARG_STARTED_AT, 0L);

        // Format device name
        String deviceLine = TextUtils.isEmpty(device)
                ? getString(R.string.workout_device_unknown)
                : getString(R.string.workout_device_fmt, device);


        tvWorkoutDevice.setText(deviceLine);
        tvWorkoutStarted.setText(timeLine);
    }

    /**
     * Update workout information dynamically if needed
     */
    public void updateWorkoutInfo(String deviceName, long startTime) {
        if (tvWorkoutDevice != null && tvWorkoutStarted != null) {
            String deviceLine = TextUtils.isEmpty(deviceName)
                    ? getString(R.string.workout_device_unknown)
                    : getString(R.string.workout_device_fmt, deviceName);

            String timeLine = startTime == 0L
                    ? getString(R.string.workout_started_fmt)
                    : getString(R.string.workout_started_fmt,
                    DateFormat.getTimeInstance(DateFormat.SHORT)
                            .format(new Date(startTime)));

            tvWorkoutDevice.setText(deviceLine);
            tvWorkoutStarted.setText(timeLine);
        }
    }
}
