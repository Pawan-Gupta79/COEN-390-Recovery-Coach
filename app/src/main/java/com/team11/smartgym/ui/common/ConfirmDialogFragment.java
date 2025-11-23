package com.team11.smartgym.ui.common;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ConfirmDialogFragment extends DialogFragment {

    public interface ConfirmListener {
        void onConfirmed();
    }

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_POSITIVE = "positive";
    private static final String ARG_NEGATIVE = "negative";

    private ConfirmListener listener;

    public static ConfirmDialogFragment newInstance(
            String title,
            String message,
            String positive,
            String negative,
            ConfirmListener listener
    ) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_POSITIVE, positive);
        args.putString(ARG_NEGATIVE, negative);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        return new AlertDialog.Builder(requireContext())
                .setTitle(args.getString(ARG_TITLE))
                .setMessage(args.getString(ARG_MESSAGE))
                .setPositiveButton(args.getString(ARG_POSITIVE), (dialog, which) -> {
                    if (listener != null) listener.onConfirmed();
                })
                .setNegativeButton(args.getString(ARG_NEGATIVE), null)
                .create();
    }
}
