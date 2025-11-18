package com.team11.smartgym.ui;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.ble.BleService;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.team11.smartgym.R;
import com.team11.smartgym.ble.DeviceItem;
import com.team11.smartgym.ble.DeviceListAdapter;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.model.ConnectionState;
import com.team11.smartgym.shared.Bus;
import com.team11.smartgym.ui.common.SnackbarUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DashboardFragment extends Fragment {

    private Chip chipDevice;
    private LineChart chart;
    private TextView tvBpm, tvState;
    private MaterialButton btnConnect, btnDisconnect, btnStartWorkout;
    private MaterialSwitch switchFake;
    private int sampleIdx = 0;
    private DashboardViewModel vm;
    private AppPrefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> scanLauncher;

    // Core Bluetooth objects
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;

    // List of discovered BLE devices shown in the dialog
    private final List<DeviceItem> foundDevices = new ArrayList<>();
    private DeviceListAdapter deviceListAdapter;
    private AlertDialog scanDialog;

    // ---- UUIDs exposed by the BLE device (must match Arduino sketch) ----
    private static final UUID HR_SERVICE_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef0");  // Custom Heart Rate–like service
    private static final UUID STANDARD_HR_SERVICE_UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"); // official BLE Heart Rate
    private static final UUID HR_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef1");  // Characteristic that holds BPM data
    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");  // Standard Client Characteristic Config (CCCD)

    // Request code for runtime permission dialog
    private static final int REQ_PERMISSIONS = 42;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        // Wire up views
        chipDevice = v.findViewById(R.id.chipDevice);
        chart = v.findViewById(R.id.chart);
        tvBpm = v.findViewById(R.id.tvBpm);
        tvState = v.findViewById(R.id.tvState);
        btnConnect = v.findViewById(R.id.btnConnect);
        btnDisconnect = v.findViewById(R.id.btnDisconnect);
        btnStartWorkout = v.findViewById(R.id.btnStartWorkout);
        switchFake = v.findViewById(R.id.switchFake);

        // Get system Bluetooth adapter
        BluetoothManager manager =
                (BluetoothManager) requireContext()
                        .getSystemService(Context.BLUETOOTH_SERVICE);

        bluetoothAdapter = (manager != null) ? manager.getAdapter() : null;

        // Main button: check permissions/Bluetooth and then start scanning
        btnConnect.setOnClickListener(view -> {
            if (!hasAllPermissions()) {
                // Ask user for required runtime permissions
                requestAllPermissions();
            } else {
                // Ensure Bluetooth itself is enabled
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    Toast.makeText(requireContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Open dialog to scan and pick a device
                showScanDialog();
            }
        });

        btnDisconnect.setOnClickListener(view -> {
            if (bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "No BLUETOOTH_CONNECT permission", Toast.LENGTH_SHORT).show();
                    return;
                }

                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
                tvState.setText("Disconnected");
                tvBpm.setText("--");
                Toast.makeText(requireContext(), "Disconnected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No device connected", Toast.LENGTH_SHORT).show();
            }
        });
        return v;
    }





    // ------------------------------------------------------
    // Permissions
    // ------------------------------------------------------

    /**
     * Checks if all required Bluetooth/location permissions are granted
     * and Bluetooth is enabled.
     */
    private boolean hasAllPermissions() {
        // If adapter is missing or disabled, treat as "no"
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return false;
        }

        // Android 12+ requires BLUETOOTH_SCAN + BLUETOOTH_CONNECT + location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Older devices only need location for BLE scanning
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests the appropriate set of permissions depending on Android version.
     */
    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQ_PERMISSIONS
            );
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_PERMISSIONS
            );
        }
    }

    /**
     * Called when the user responds to the permission dialog.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_PERMISSIONS) {
            if (hasAllPermissions()) {
                // If everything is granted, go straight to scanning
                showScanDialog();
            } else {
                Toast.makeText(requireContext(), "Permissions denied, BLE won’t work", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ------------------------------------------------------
    // Scan dialog
    // ------------------------------------------------------

    /**
     * Shows a dialog listing discovered BLE devices and starts scanning.
     */
    private void showScanDialog() {
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth not available", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get LE scanner from adapter
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Toast.makeText(requireContext(), "BLE scanner not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset previous scan results
        foundDevices.clear();
        deviceListAdapter = new DeviceListAdapter(requireContext(), foundDevices);

        tvState.setText("Scanning for BLE devices...");

        // Dialog shows devices as they are discovered
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select BLE Device");

        // When the user taps an entry, stop scanning and connect
        builder.setAdapter(deviceListAdapter, (dialog, which) -> {
            stopScan();
            DeviceItem item = foundDevices.get(which);
            connectToDevice(item.device);
        });

        // User can cancel scan
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            stopScan();
            tvState.setText("Scan cancelled");
        });

        scanDialog = builder.create();
        scanDialog.show();

        // Begin BLE scan
        startScan();
    }

    /**
     * Starts a BLE scan filtered by our custom HR service UUID.
     */
    private void startScan() {
        if (bleScanner == null) return;

        // Ensure we still have scan permission (mainly Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "No BLUETOOTH_SCAN permission", Toast.LENGTH_SHORT).show();
            return;
        }

        // Filter so we primarily see devices that advertise our HR service
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new android.os.ParcelUuid(HR_SERVICE_UUID))
                .build();

        // Low latency = faster results, more power usage
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bleScanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        tvState.setText("Scanning...");
    }

    /**
     * Stops an ongoing BLE scan if active.
     */
    private void stopScan() {
        if (bleScanner == null) return;

        // Permission check required for stopScan on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            bleScanner.stopScan(scanCallback);
        } catch (Exception ignored) {
            // Swallow any internal scanner exceptions
        }
    }

    /**
     * Callback that receives scan results and updates the device list.
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice dev = result.getDevice();
            if (dev == null) return;

            String name = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                name = dev.getName();
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                name = dev.getName(); // old Android doesn't require permission
            }
            if (name == null && result.getScanRecord() != null) {
                name = result.getScanRecord().getDeviceName();
            }
            if (name == null || name.isEmpty()) {
                name = "(Unknown)";
            }


            // Avoid adding the same device twice
            for (DeviceItem item : foundDevices) {
                if (item.address.equals(dev.getAddress())) {
                    return;
                }
            }

            // Wrap raw device into our DeviceItem model
            DeviceItem item = new DeviceItem(name, dev.getAddress(), dev);
            foundDevices.add(item);
            // Update UI to reflect the new device count
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvState.setText("Found " + foundDevices.size() + " device(s)");
                    if (deviceListAdapter != null) deviceListAdapter.notifyDataSetChanged();
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Show scan error in status text
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        tvState.setText("Scan failed: " + errorCode)
                );
            }
        }
    };

// ------------------------------------------------------
        // GATT connection & Heart Rate notifications
        // ------------------------------------------------------

        /**
         * Initiates a GATT connection to the selected BLE device.
         */
        private void connectToDevice(BluetoothDevice device) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                tvState.setText("Connecting to " + device.getName() + "...");
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                tvState.setText("Connecting to " + device.getName() + "...");
            }

            // Android 12+ requires BLUETOOTH_CONNECT permission to connect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "No BLUETOOTH_CONNECT permission", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use LE-only transport where available, fallback for older versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback,
                        BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback);
            }
        }

        /**
         * Handles connection events, service discovery, and incoming HR updates.
         */
        private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

            /**
             * Called when the connection state changes (connected/disconnected).
             */
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // Once connected, start discovering services on the device
                    getActivity().runOnUiThread(() -> tvState.setText("Connected. Discovering services..."));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                                == PackageManager.PERMISSION_GRANTED) {
                            gatt.discoverServices();
                        } else {
                            //request permission here
                            Toast.makeText(requireContext(), "Missing BLUETOOTH_CONNECT permission", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Older Android versions don't need explicit permission for this
                        gatt.discoverServices();
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    // Reset UI when disconnected
                    getActivity().runOnUiThread(() -> {
                        tvState.setText("Disconnected");
                        tvBpm.setText("--");
                    });
                }
            }

            /**
             * Called when all services are discovered on the remote device.
             */
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                // Look up our custom HR service
                BluetoothGattService svc = gatt.getService(HR_SERVICE_UUID);
                if (svc == null) {
                    getActivity().runOnUiThread(() -> tvState.setText("HR service not found"));
                    return;
                }

                // Look up the characteristic that contains BPM data
                BluetoothGattCharacteristic hrChar = svc.getCharacteristic(HR_CHAR_UUID);
                if (hrChar == null) {
                    getActivity().runOnUiThread(() -> tvState.setText("HR characteristic not found"));
                    return;
                }

                // Enable notifications for the HR characteristic
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                        gatt.setCharacteristicNotification(hrChar, true);
                    } else {
                        //request permission here
                        gatt.setCharacteristicNotification(hrChar, true);
                    }
                } else {
                    // Older Android versions don't need explicit permission for this
                    gatt.setCharacteristicNotification(hrChar, true);
                }


                // Write to its CCCD descriptor so the peripheral actually pushes updates
                BluetoothGattDescriptor cccd = hrChar.getDescriptor(CCCD_UUID);
                if (cccd != null) {
                    cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(cccd);
                }

                getActivity().runOnUiThread(() -> tvState.setText("Receiving BPM..."));
            }

            /**
             * Called whenever the HR characteristic sends a new value (notification).
             */
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if (HR_CHAR_UUID.equals(characteristic.getUuid())) {
                    // Read the first byte as an unsigned 8-bit integer = BPM
                    int bpm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

                    // Update BPM UI on the main thread
                    getActivity().runOnUiThread(() -> tvBpm.setText(String.valueOf(bpm)));
                }
            }
        };

        @Override
        public void onDestroy() {
            super.onDestroy();

            // Ensure we stop scanning and release GATT resources when activity is destroyed
            stopScan();
            if (bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothGatt.close();
                    }
                } else {
                    bluetoothGatt.close();
                }
                bluetoothGatt = null;
            }


        }



}
