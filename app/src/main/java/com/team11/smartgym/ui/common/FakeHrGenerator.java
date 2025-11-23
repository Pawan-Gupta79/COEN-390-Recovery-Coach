package com.team11.smartgym.ui.common;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;

/**
 * Simple periodic HR generator (1 Hz).
 * Call start() to begin callbacks; stop() to end.
 */
public class FakeHrGenerator {

    public interface Listener {
        void onBpm(int bpm);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final Listener listener;

    private boolean running = false;
    private int current = 75;          // start around 75 bpm

    public FakeHrGenerator(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        tick.run();
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;

            // wander a little with bounds [60..110]
            int delta = rnd.nextInt(5) - 2;      // -2..+2
            current = Math.max(60, Math.min(110, current + delta));
            if (listener != null) {
                listener.onBpm(current);
            }
            handler.postDelayed(this, 1000); // 1 Hz
        }
    };
}
