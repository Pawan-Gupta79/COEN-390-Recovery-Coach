package com.team11.smartgym.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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

    private static final String TAG = "DeviceScanActivity";
    private ActivityResultLauncher<Intent> enableBtLauncher;
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private final String[] REQUIRED_PERMISSIONS_BLE = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        b = ActivityDeviceScanBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK) {

                        initializeBluetooth();
                    } else {

                        Toast.makeText(this, "Bluetooth must be enabled to scan for devices.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        );

        EdgeToEdge.enable(this);
        b = ActivityDeviceScanBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        if (checkAndRequestPermissions()) {
            initializeBluetooth();
        }
    }

    private void initializeBluetooth() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter ba = bm.getAdapter();

        if (ba == null) {
            Toast.makeText(this, "Device does not support Bluetooth.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!ba.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
            return;
        }

        BluetoothLeScanner bluetoothLeScanner = ba.getBluetoothLeScanner();

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null. Cannot start scan. (Adapter issue)");
            Toast.makeText(this, "BLE not available or active.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        scanner = bluetoothLeScanner;

        adapter = new DeviceAdapter(this, results, r -> {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Connect permission denied.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(this, BleService.class)
                    .setAction(BleService.ACTION_CONNECT)
                    .putExtra(BleService.EXTRA_DEVICE, ((ScanResult) r).getDevice().getAddress());
            startService(i);
            Snackbar.make(b.getRoot(), "Connecting to " + ((ScanResult) r).getDevice().getName(), Snackbar.LENGTH_SHORT).show();
            finish();
        });
        b.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        b.rvDevices.setAdapter(adapter);

        b.btnScan.setOnClickListener(v -> toggleScan());

        toggleScan();
    }

    private void toggleScan() {
        if (scanner == null || checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Cannot scan: Scanner not ready or permission denied.", Toast.LENGTH_SHORT).show();
            scanning = false;
            return;
        }

        if (scanning) {
            scanner.stopScan(cb);
            b.progress.setVisibility(View.GONE);
            scanning = false;
        } else {
            results.clear(); adapter.notifyDataSetChanged();
            b.progress.setVisibility(View.VISIBLE);
            scanning = true;

            List<ScanFilter> filters = new ArrayList<>();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanner.startScan(filters, settings, cb);

            handler.postDelayed(() -> { if (scanning) toggleScan(); }, 10000);
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS_BLE) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.isEmpty()) {
            return true;
        } else {
            requestPermissions(
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS
            );
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "Permissions required for Bluetooth scanning were denied.", Toast.LENGTH_LONG).show();
                finish();
            }
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
        private final Context context;
        interface OnPick { void pick(ScanResult r); }
        private final List<ScanResult> list;
        private final OnPick onPick;
        DeviceAdapter(Context context, List<ScanResult> list, OnPick onPick) {
            this.list = list;
            this.context = context;
            this.onPick = onPick; }

        @NonNull @Override public DeviceVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new DeviceVH(v);
        }

        @Override public void onBindViewHolder(@NonNull DeviceVH h, int pos) {
            ScanResult r = list.get(pos);

            String deviceName;

            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                deviceName = r.getDevice().getName() == null ? "(unknown)" : r.getDevice().getName();
            } else {
                deviceName = "(Permission Denied)";
            }

            h.t1.setText(deviceName);
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