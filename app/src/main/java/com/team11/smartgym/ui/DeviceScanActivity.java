package com.team11.smartgym.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.team11.smartgym.R;
import com.team11.smartgym.ui.common.SnackbarUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeviceScanActivity extends AppCompatActivity {

    // ---- Result extras used by DashboardFragment
    public static final String EXTRA_DEVICE_NAME = "extra_device_name";
    public static final String EXTRA_DEVICE_ADDR = "extra_device_addr";

    private TextInputEditText etFilter;
    private MaterialButton btnScan;
    private ProgressBar progress;
    private RecyclerView rvDevices;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<DeviceItem> devices = new ArrayList<>();
    private DevicesAdapter adapter;

    private boolean isScanning = false;
    private String currentFilter = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        etFilter = findViewById(R.id.etFilter);
        btnScan = findViewById(R.id.btnScan);
        progress = findViewById(R.id.progress);
        rvDevices = findViewById(R.id.rvDevices);

        // RecyclerView setup
        adapter = new DevicesAdapter(devices, item -> {
            if (isScanning) stopScan(false);
            // Return selection
            Intent data = new Intent();
            data.putExtra(EXTRA_DEVICE_NAME, item.name);
            data.putExtra(EXTRA_DEVICE_ADDR, item.addr);
            setResult(RESULT_OK, data);
            finish();
        });
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);
        rvDevices.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Filter change
        etFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentFilter = s == null ? "" : s.toString().trim();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Scan button
        btnScan.setOnClickListener(v -> {
            if (isScanning) {
                showStopScanDialog();
            } else {
                startScan();
            }
        });
    }

    // Intercept back: if scanning, confirm stop first
    @Override
    public void onBackPressed() {
        if (isScanning) {
            showStopScanDialog(/* onPositive */() -> {
                stopScan(true);
                // After stopping, actually go back
                DeviceScanActivity.super.onBackPressed();
            });
        } else {
            super.onBackPressed();
        }
    }

    // -------------------- Scanning (simulated) --------------------

    private void startScan() {
        isScanning = true;
        progress.setVisibility(View.VISIBLE);
        devices.clear();
        adapter.notifyDataSetChanged();
        SnackbarUtil.show(this, getString(R.string.scanning));

        // Simulate ~3s scan then populate results
        handler.postDelayed(this::finishScanWithResults, 1500);
        handler.postDelayed(this::finishScanWithResults, 3000);
    }

    private void finishScanWithResults() {
        if (!isScanning) return;

        // Simulated pool (pretend scan found some)
        List<DeviceItem> pool = new ArrayList<>();
        pool.add(new DeviceItem("SmartGym HR-1A", "AA:BB:01"));
        pool.add(new DeviceItem("SmartGym HR-2B", "AA:BB:02"));
        pool.add(new DeviceItem("Polar H10",      "AA:BB:10"));
        pool.add(new DeviceItem("Garmin HRM",     "AA:BB:11"));

        // Apply simple prefix filter on name if provided
        for (DeviceItem di : pool) {
            if (currentFilter.isEmpty() || di.name.toLowerCase(Locale.US).startsWith(currentFilter.toLowerCase(Locale.US))) {
                // Avoid duplicates
                boolean exists = false;
                for (DeviceItem e : devices) {
                    if (e.addr.equals(di.addr)) { exists = true; break; }
                }
                if (!exists) devices.add(di);
            }
        }
        adapter.notifyDataSetChanged();

        // On last batch, auto-stop scanning and show count
        // If we already had at least one round, stop now:
        if (devices.size() > 0) {
            stopScan(false);
            SnackbarUtil.show(this, "Found " + devices.size() + " devices");
        }
    }

    private void stopScan(boolean fromUser) {
        isScanning = false;
        progress.setVisibility(View.GONE);
        if (fromUser) {
            SnackbarUtil.show(this, getString(R.string.stop)); // “Stop” string already exists
        }
    }

    // -------------------- Stop Scan Dialog --------------------

    private void showStopScanDialog() {
        showStopScanDialog(null);
    }

    private void showStopScanDialog(@Nullable Runnable onStopConfirmed) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setIcon(android.R.drawable.ic_media_pause) // “stop” style icon
                .setTitle(R.string.stop_scan_title)
                .setMessage(R.string.stop_scan_msg)
                .setNegativeButton(R.string.keep_scanning, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.stop, (d, w) -> {
                    stopScan(true);
                    if (onStopConfirmed != null) onStopConfirmed.run();
                });

        AlertDialog dialog = builder.show();

        // Tint the STOP button red for emphasis
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        });
    }

    // -------------------- Adapter / ViewHolder --------------------

    private static class DeviceItem {
        final String name;
        final String addr;
        DeviceItem(String name, String addr) {
            this.name = name;
            this.addr = addr;
        }
    }

    private static class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

        interface OnClick {
            void onItem(DeviceItem item);
        }

        private final List<DeviceItem> items;
        private final OnClick onClick;

        DevicesAdapter(List<DeviceItem> items, OnClick onClick) {
            this.items = items;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new DeviceViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            DeviceItem di = items.get(position);
            android.widget.TextView t1 = holder.itemView.findViewById(android.R.id.text1);
            android.widget.TextView t2 = holder.itemView.findViewById(android.R.id.text2);
            t1.setText(di.name);
            t2.setText(di.addr);
            holder.itemView.setOnClickListener(v -> onClick.onItem(di));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class DeviceViewHolder extends RecyclerView.ViewHolder {
            DeviceViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
