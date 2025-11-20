package com.team11.smartgym.debug;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.team11.smartgym.R;
import com.team11.smartgym.ble.DeviceItem;
import com.team11.smartgym.ble.DeviceListAdapter;

public class BluetoothDebugScanActivity extends AppCompatActivity {

    // ---- UUIDs (must match Arduino) ----
    private static final UUID HR_SERVICE_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef0");

    private static final UUID HR_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef1");
    private static final UUID TEMP_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef2");
    private static final UUID HUM_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef3");
    private static final UUID CONTROL_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef4");
    private static final UUID STEPS_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef5");

    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic hrCharacteristic;
    private BluetoothGattCharacteristic tempCharacteristic;
    private BluetoothGattCharacteristic humCharacteristic;
    private BluetoothGattCharacteristic controlCharacteristic;

    private final List<DeviceItem> foundDevices = new ArrayList<>();
    private DeviceListAdapter deviceListAdapter;
    private AlertDialog scanDialog;

    private TextView tvStatus;
    private TextView tvHeartRate;
    private TextView tvTemp;
    private TextView tvHumidity;
    private TextView tvSteps;
    private Button btnScan;
    private Button btnConnect;
    private Button btnStartSampling;
    private Button btnStopSampling;
    private Button btnOpenWorkout;

    private String connectedDeviceAddress;
    private String connectedDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_scan);

        tvStatus = findViewById(R.id.tvStatus);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvTemp = findViewById(R.id.tvTemp);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvSteps = findViewById(R.id.tvSteps);
        btnScan = findViewById(R.id.btnScan);
        btnConnect = findViewById(R.id.btnConnect);
        btnStartSampling = findViewById(R.id.btnStartSampling);
        btnStopSampling = findViewById(R.id.btnStopSampling);
        btnOpenWorkout = findViewById(R.id.btnOpenWorkout);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        deviceListAdapter = new DeviceListAdapter(this, foundDevices);

        btnScan.setOnClickListener(v -> {
            if (!checkAndRequestPermissions()) return;

            if (!isScanning) {
                startScan();
            } else {
                stopScan();
            }
        });

        btnConnect.setOnClickListener(v -> {
            if (connectedDeviceAddress == null) {
                showDevicePickerDialog();
            } else {
                reconnectToLastDevice();
            }
        });

        btnStartSampling.setOnClickListener(v -> {
            if (bluetoothGatt == null || controlCharacteristic == null) {
                Toast.makeText(this, "Connect to the board first", Toast.LENGTH_SHORT).show();
                return;
            }
            sendStartCommand();
        });

        btnStopSampling.setOnClickListener(v -> {
            if (bluetoothGatt == null || controlCharacteristic == null) {
                Toast.makeText(this, "Connect to the board first", Toast.LENGTH_SHORT).show();
                return;
            }
            sendStopCommand();
        });

        btnOpenWorkout.setOnClickListener(v -> {
            if (connectedDeviceAddress == null) {
                Toast.makeText(this, "Connect to the board first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Free this GATT so WorkoutActivity can own the connection
            if (bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                                == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt.disconnect();
                }
                bluetoothGatt.close();
                bluetoothGatt = null;
            }

            Intent intent = new Intent(BluetoothDebugScanActivity.this, BluetoothDebugWorkoutActivity.class);
            intent.putExtra("device_address", connectedDeviceAddress);
            intent.putExtra("device_name", connectedDeviceName);
            startActivity(intent);
        });

        updateUiDisconnected();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> needed = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void startScan() {
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "BLE scanner not available", Toast.LENGTH_SHORT).show();
            return;
        }

        foundDevices.clear();
        deviceListAdapter.notifyDataSetChanged();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(HR_SERVICE_UUID))
                .build());

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        tvStatus.setText("Scanning...");
        btnScan.setText("Stop Scan");
        isScanning = true;

        bluetoothLeScanner.startScan(filters, settings, scanCallback);
    }

    private void stopScan() {
        if (bluetoothLeScanner != null && isScanning) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
        isScanning = false;
        btnScan.setText("Start Scan");
        if (foundDevices.isEmpty()) {
            tvStatus.setText("Scan stopped. No devices found.");
        } else {
            tvStatus.setText("Scan stopped.");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name == null) name = "Unknown";

            String address = device.getAddress();

            for (DeviceItem item : foundDevices) {
                if (item.address.equals(address)) return;
            }

            foundDevices.add(new DeviceItem(name, address, device));
            Collections.sort(foundDevices, (a, b) -> a.name.compareToIgnoreCase(b.name));
            deviceListAdapter.notifyDataSetChanged();
        }
    };

    private void showDevicePickerDialog() {
        if (foundDevices.isEmpty()) {
            Toast.makeText(this, "No devices found. Start a scan first.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("Scanning for BLE devices...");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select BLE Device");

        builder.setAdapter(deviceListAdapter, (dialog, which) -> {
            stopScan();
            DeviceItem item = foundDevices.get(which);
            connectToDevice(item.device);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        scanDialog = builder.create();
        scanDialog.show();
    }

    private void reconnectToLastDevice() {
        if (connectedDeviceAddress == null) {
            Toast.makeText(this, "No device to reconnect", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(connectedDeviceAddress);
        connectToDevice(device);
    }

    private void connectToDevice(BluetoothDevice device) {
        tvStatus.setText("Connecting to " + device.getName() + "...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth connect permission missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread(() -> tvStatus.setText("Connected. Discovering services..."));
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    tvStatus.setText("Disconnected");
                    tvHeartRate.setText("-- bpm");
                    tvTemp.setText("-- °C");
                    tvHumidity.setText("-- %");
                    tvSteps.setText("--");
                });
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(HR_SERVICE_UUID);
            if (service == null) {
                runOnUiThread(() -> tvStatus.setText("HR service not found"));
                return;
            }

            hrCharacteristic = service.getCharacteristic(HR_CHAR_UUID);
            tempCharacteristic = service.getCharacteristic(TEMP_CHAR_UUID);
            humCharacteristic = service.getCharacteristic(HUM_CHAR_UUID);
            controlCharacteristic = service.getCharacteristic(CONTROL_CHAR_UUID);

            if (hrCharacteristic == null || tempCharacteristic == null ||
                    humCharacteristic == null || controlCharacteristic == null) {
                runOnUiThread(() -> tvStatus.setText("One or more characteristics not found"));
                return;
            }

            enableHeartRateNotifications(gatt);

            connectedDeviceAddress = gatt.getDevice().getAddress();
            connectedDeviceName = gatt.getDevice().getName();

            runOnUiThread(() -> {
                tvStatus.setText(String.format(Locale.getDefault(),
                        "Connected to %s (%s)", connectedDeviceName, connectedDeviceAddress));
                btnConnect.setText("Reconnect");
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();

            if (HR_CHAR_UUID.equals(uuid)) {
                byte[] data = characteristic.getValue();
                int bpm = (data != null && data.length > 0) ? (data[0] & 0xFF) : 0;
                runOnUiThread(() -> tvHeartRate.setText(bpm + " bpm"));
            } else if (TEMP_CHAR_UUID.equals(uuid)) {
                byte[] data = characteristic.getValue();
                float temp = 0f;
                if (data != null && data.length >= 4) {
                    int bits = (data[0] & 0xFF) |
                            ((data[1] & 0xFF) << 8) |
                            ((data[2] & 0xFF) << 16) |
                            ((data[3] & 0xFF) << 24);
                    temp = Float.intBitsToFloat(bits);
                }
                final float tempFinal = temp;
                runOnUiThread(() -> tvTemp.setText(String.format(Locale.getDefault(),
                        "%.1f °C", tempFinal)));
            } else if (HUM_CHAR_UUID.equals(uuid)) {
                byte[] data = characteristic.getValue();
                float hum = 0f;
                if (data != null && data.length >= 4) {
                    int bits = (data[0] & 0xFF) |
                            ((data[1] & 0xFF) << 8) |
                            ((data[2] & 0xFF) << 16) |
                            ((data[3] & 0xFF) << 24);
                    hum = Float.intBitsToFloat(bits);
                }
                final float humFinal = hum;
                runOnUiThread(() -> tvHumidity.setText(String.format(Locale.getDefault(),
                        "%.1f %%", humFinal)));
            } else if (STEPS_CHAR_UUID.equals(uuid)) {
                byte[] data = characteristic.getValue();
                long steps = 0;
                if (data != null && data.length >= 4) {
                    steps = ((long) (data[0] & 0xFF)) |
                            ((long) (data[1] & 0xFF) << 8) |
                            ((long) (data[2] & 0xFF) << 16) |
                            ((long) (data[3] & 0xFF) << 24);
                }
                final long stepsFinal = steps;
                runOnUiThread(() -> tvSteps.setText(String.valueOf(stepsFinal)));
            }
        }
    };

    private void enableHeartRateNotifications(BluetoothGatt gatt) {
        if (hrCharacteristic == null) return;

        gatt.setCharacteristicNotification(hrCharacteristic, true);

        BluetoothGattDescriptor cccd = hrCharacteristic.getDescriptor(CCCD_UUID);
        if (cccd != null) {
            cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(cccd);
        }
    }

    private void sendStartCommand() {
        sendControlCommand((byte) 0x10); // Example START command
    }

    private void sendStopCommand() {
        sendControlCommand((byte) 0x11); // Example STOP command
    }

    private void sendControlCommand(byte cmd) {
        if (bluetoothGatt == null || controlCharacteristic == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        controlCharacteristic.setValue(new byte[]{cmd});
        bluetoothGatt.writeCharacteristic(controlCharacteristic);
    }

    private void updateUiDisconnected() {
        tvStatus.setText("Disconnected");
        tvHeartRate.setText("-- bpm");
        tvTemp.setText("-- °C");
        tvHumidity.setText("-- %");
        tvSteps.setText("--");
        btnConnect.setText("Connect");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int gr : grantResults) {
                if (gr != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Permissions are required for BLE", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
