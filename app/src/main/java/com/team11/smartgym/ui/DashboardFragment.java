package com.team11.smartgym.ui;

import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.*;
import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
import com.team11.smartgym.ble.BleService;
import com.team11.smartgym.databinding.FragmentDashboardBinding;
import com.team11.smartgym.sensors.FakeSensor;
import com.team11.smartgym.shared.Bus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding b;
    private FakeSensor fake;
    private final Handler h = new Handler(Looper.getMainLooper());
    private int sampleIdx = 0;
    private int lastBpm = -1;

    private final BroadcastReceiver bus = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (Bus.ACTION_HR_UPDATE.equals(a)) {
                int bpm = intent.getIntExtra(Bus.EXTRA_BPM, -1);
                lastBpm = bpm;
                b.tvBpm.setText(getString(R.string.hr_bpm, bpm));
                addPoint(bpm);
            } else if (Bus.ACTION_STATE.equals(a)) {
                String s = intent.getStringExtra(Bus.EXTRA_STATE);
                b.tvState.setText(s);
                b.chipDevice.setText(s);
            } else if (Bus.ACTION_ERROR.equals(a)) {
                Snackbar.make(b.getRoot(), intent.getStringExtra(Bus.EXTRA_ERROR), Snackbar.LENGTH_LONG).show();
            }
        }
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentDashboardBinding.inflate(inflater, container, false);
        initChart(b.chart);
        b.btnConnect.setOnClickListener(v -> {
            if (b.switchFake.isChecked()) startFake();
            else startBleFlow();
        });
        b.btnDisconnect.setOnClickListener(v -> {
            stopFake();
            requireContext().stopService(new Intent(requireContext(), BleService.class));
        });
        return b.getRoot();
    }

    @Override public void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(Bus.ACTION_HR_UPDATE);
        f.addAction(Bus.ACTION_STATE);
        f.addAction(Bus.ACTION_ERROR);
        requireContext().registerReceiver(bus, f, Context.RECEIVER_EXPORTED);
        h.post(tick);
    }

    @Override public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(bus);
        h.removeCallbacksAndMessages(null);
    }

    private void startBleFlow() {
        requireContext().startService(new Intent(requireContext(), BleService.class).setAction(BleService.ACTION_START));
        startActivity(new Intent(requireContext(), DeviceScanActivity.class));
    }

    private void startFake() {
        if (fake == null) fake = new FakeSensor(requireContext());
        fake.start();
        b.tvState.setText("Simulated");
        b.chipDevice.setText("Simulated");
    }
    private void stopFake() {
        if (fake != null) fake.stop();
        lastBpm = -1;
        b.tvState.setText("Disconnected");
        b.chipDevice.setText("Disconnected");
    }

    private void initChart(LineChart chart) {
        Description d = new Description(); d.setText("Last 60s"); chart.setDescription(d);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setDrawLabels(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(40f);
        chart.getAxisLeft().setAxisMaximum(200f);

        LineDataSet ds = new LineDataSet(new ArrayList<>(), "HR");
        ds.setDrawCircles(false);
        ds.setLineWidth(2.5f);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        chart.setData(new LineData(ds));
        chart.animateX(300);
    }

    private void addPoint(int bpm) {
        LineData data = b.chart.getData();
        LineDataSet ds = (LineDataSet) data.getDataSetByIndex(0);
        ds.addEntry(new Entry(sampleIdx++, bpm));
        while (ds.getEntryCount() > 60) ds.removeFirst();
        data.notifyDataChanged();
        b.chart.notifyDataSetChanged();
        b.chart.moveViewToX(sampleIdx);
        b.chart.invalidate();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (lastBpm > 0) addPoint(lastBpm);
            h.postDelayed(this, 1000);
        }
    };
}