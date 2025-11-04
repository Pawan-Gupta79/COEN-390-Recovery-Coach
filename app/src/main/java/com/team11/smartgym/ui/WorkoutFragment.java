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

public class WorkoutFragment extends Fragment {

    public static final String ARG_DEVICE_NAME = "arg_device_name";
    public static final String ARG_STARTED_AT  = "arg_started_at";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_workout, container, false);

        TextView tvDevice  = v.findViewById(R.id.tvWorkoutDevice);
        TextView tvStarted = v.findViewById(R.id.tvWorkoutStarted);

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

        return v;
    }
}
