package com.team11.smartgym.sensors;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.team11.smartgym.shared.Bus;

import java.util.Random;

public class FakeSensor {
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random r = new Random();
    private final Context ctx;
    private boolean running = false;
    private int base = 72;

    public FakeSensor(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            int bpm = base + (int) Math.round(Math.sin(System.currentTimeMillis() / 1500.0) * 8) + r.nextInt(3);
            Bus.sendHr(ctx, bpm);
            Bus.sendState(ctx, "Simulated");
            h.postDelayed(this, 1000);
        }
    };

    public void start() {
        if (running) return;
        running = true;
        h.post(tick);
    }

    public void stop() {
        running = false;
        h.removeCallbacksAndMessages(null);
    }
}