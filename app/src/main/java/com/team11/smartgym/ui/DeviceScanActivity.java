package com.team11.smartgym.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
import com.team11.smartgym.ble.BleService;
import com.team11.smartgym.databinding.ActivityDeviceScanBinding;

import java.util.*;

public class DeviceScanActivity extends AppCompatActivity {

    private ActivityDeviceScanBinding b;
    private BluetoothLeScanner scanner;
    private final List<ScanResult> results = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean scanning = false;
    private DeviceAdapter adapter;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        b = ActivityDeviceScanBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter ba = bm.getAdapter();
        scanner = ba.getBluetoothLeScanner();

        adapter = new DeviceAdapter(results, r -> {
            Intent i = new Intent(this, BleService.class)
                    .setAction(BleService.ACTION_CONNECT)
                    .putExtra(BleService.EXTRA_DEVICE, r.getDevice().getAddress());
            startService(i);
            Snackbar.make(b.getRoot(), "Connecting to " + r.getDevice().getName(), Snackbar.LENGTH_SHORT).show();
            finish();
        });
        b.rvDevices.setAdapter(adapter);

        b.btnScan.setOnClickListener(v -> toggleScan());
    }

    private void toggleScan() {
        if (scanning) {
            scanner.stopScan(cb);
            b.progress.setVisibility(View.GONE);
            scanning = false;
        } else {
            results.clear(); adapter.notifyDataSetChanged();
            b.progress.setVisibility(View.VISIBLE);
            scanning = true;

            List<ScanFilter> filters = new ArrayList<>();
            // optional prefix filter via setDeviceName() isn't reliable; keeping open scan
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanner.startScan(filters, settings, cb);

            handler.postDelayed(() -> { if (scanning) toggleScan(); }, 10000);
        }
    }

    private final ScanCallback cb = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            for (ScanResult r : results) {
                if (r.getDevice().getAddress().equals(result.getDevice().getAddress())) return;
            }
            results.add(result);
            adapter.notifyItemInserted(results.size() - 1);
        }
    };

    static class DeviceAdapter extends RecyclerView.Adapter<DeviceVH> {
        interface OnPick { void pick(ScanResult r); }
        private final List<ScanResult> list;
        private final OnPick onPick;
        DeviceAdapter(List<ScanResult> list, OnPick onPick) { this.list = list; this.onPick = onPick; }

        @NonNull @Override public DeviceVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new DeviceVH(v);
        }

        @Override public void onBindViewHolder(@NonNull DeviceVH h, int pos) {
            ScanResult r = list.get(pos);
            h.t1.setText(r.getDevice().getName() == null ? "(unknown)" : r.getDevice().getName());
            h.t2.setText(r.getDevice().getAddress() + "  RSSI " + r.getRssi());
            h.itemView.setOnClickListener(v -> onPick.pick(r));
        }

        @Override public int getItemCount() { return list.size(); }
    }

    static class DeviceVH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        DeviceVH(@NonNull View itemView) {
            super(itemView);
            t1 = itemView.findViewById(android.R.id.text1);
            t2 = itemView.findViewById(android.R.id.text2);
        }
    }
}