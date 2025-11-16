package com.team11.smartgym.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.team11.smartgym.ble.BleService;
import com.team11.smartgym.databinding.ActivityDeviceScanBinding;
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
    private final List<ScanResult> devices = new ArrayList<>();
    private DevicesAdapter adapter;

    private boolean isScanning = false;
    private String currentFilter = "";

    //Config between real and fake scan
    private static final boolean USE_SIMULATION = true;
    //BLE objects
    private ActivityDeviceScanBinding b;
    private BluetoothLeScanner scanner;

    private static final String TAG = "DeviceScanActivity";
    private ActivityResultLauncher<Intent> enableBtLauncher;
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private final String[] REQUIRED_PERMISSIONS_BLE = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_scan);

        etFilter = findViewById(R.id.etFilter);
        btnScan = findViewById(R.id.btnScan);
        progress = findViewById(R.id.progress);
        rvDevices = findViewById(R.id.rvDevices);

        // RecyclerView setup


        // Filter change


        // Scan button


        //Set up BLE
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

    // Intercept back: if scanning, confirm stop first

    // -------------------- Scanning (simulated) --------------------




    // -------------------- Stop Scan Dialog --------------------



    // -------------------- Adapter / ViewHolder --------------------



    private static class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {
        private final Context context;
        interface OnClick {
            void onItem(ScanResult item);
        }

        private final List<ScanResult> items;
        private final OnClick onClick;

        DevicesAdapter(Context context, List<ScanResult> items, OnClick onClick) {
            this.items = items;
            this.onClick = onClick;
            this.context = context;
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
            ScanResult di = items.get(position);
            String deviceName;
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                deviceName = di.getDevice().getName() == null ? "(unknown)" : di.getDevice().getName();
            } else {
                deviceName = "(Permission Denied)";
            }

            android.widget.TextView t1 = holder.itemView.findViewById(android.R.id.text1);
            android.widget.TextView t2 = holder.itemView.findViewById(android.R.id.text2);
            t1.setText(deviceName);
            t2.setText(di.getDevice().getAddress() + " RSSI " + di.getRssi());
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

        adapter = new DevicesAdapter(this, devices, item -> {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Connect permission denied.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent i = new Intent(this, BleService.class)
                    .setAction(BleService.ACTION_CONNECT)
                    .putExtra(BleService.EXTRA_DEVICE, ((ScanResult) item).getDevice().getAddress());
            startService(i);
            Snackbar.make(b.getRoot(), "Connecting to " + ((ScanResult) item).getDevice().getName(), Snackbar.LENGTH_SHORT).show();
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
            isScanning = false;
            return;
        }

        if (isScanning) {
            scanner.stopScan(cb);
            b.progress.setVisibility(View.GONE);
            isScanning = false;

        } else {
            devices.clear();
            adapter.notifyDataSetChanged();
            b.progress.setVisibility(View.VISIBLE);
            isScanning = true;

            List<ScanFilter> filters = new ArrayList<>();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanner.startScan(filters, settings, cb);

            handler.postDelayed(() -> {
                if (isScanning) toggleScan();
            }, 10000);
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
        @Override public void onScanResult(int callbackType, ScanResult device) {
            for (ScanResult d : devices) {
                if (d.getDevice().getAddress().equals(device.getDevice().getAddress())) return;
            }
            devices.add(device);
            adapter.notifyItemInserted(devices.size() - 1);
        }
    };



}
