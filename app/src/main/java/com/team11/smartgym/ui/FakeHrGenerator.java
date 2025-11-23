package com.team11.smartgym.ui;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;
import java.util.function.Consumer;

/** Simple fake HR source that produces a value every ~300 ms. */
public class FakeHrGenerator {
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final Consumer<Integer> sink;
    private boolean running = false;
    private int base = 72;

    public FakeHrGenerator(Consumer<Integer> sink) {
        this.sink = sink;
    }

    public void start() {
        if (running) return;
        running = true;
        h.post(tick);
    }

    public void stop() {
        running = false;
        h.removeCallbacksAndMessages(null);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            base += rnd.nextInt(5) - 2;                 // -2..+2
            int v = Math.max(50, Math.min(160, base));  // clamp
            sink.accept(v);
            h.postDelayed(this, 300L);
        }
    };
}

