package com.team11.smartgym.ui.common;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import com.google.android.material.snackbar.Snackbar;

public class SnackbarUtil {

    /** Preferred in Fragments: anchor to a real view */
    public static void show(View anchor, String message) {
        if (anchor != null) {
            Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Fallback for Activities */
    public static void show(Context context, String message) {
        if (context instanceof Activity) {
            View root = ((Activity) context).findViewById(android.R.id.content);
            if (root != null) {
                Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}


