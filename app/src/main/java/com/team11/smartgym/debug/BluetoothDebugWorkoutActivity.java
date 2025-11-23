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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.team11.smartgym.R;

public class BluetoothDebugWorkoutActivity extends AppCompatActivity {

    private static final UUID HR_SERVICE_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef0");
    private static final UUID HR_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef1");
    private static final UUID CONTROL_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef4");
    private static final UUID STEPS_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef5");
    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final byte CMD_START_IMU = 0x10;
    private static final byte CMD_STOP_IMU  = 0x11;

    private static final float CALORIES_PER_STEP = 0.04f;

    private TextView tvDeviceName;
    private TextView tvTimer;
    private TextView tvSteps;
    private TextView tvHrWorkout;
    private TextView tvCalories;
    private TextView tvWorkoutStatus;

    private Button btnStart;
    private Button btnPause;
    private Button btnStop;

    private LinearLayout logContainer;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic hrCharacteristic;
    private BluetoothGattCharacteristic stepsCharacteristic;
    private BluetoothGattCharacteristic controlCharacteristic;

    private boolean isWorkoutRunning = false;
    private boolean isWorkoutPaused = false;

    private long workoutStartTimeMs = 0;
    private long pauseStartTimeMs = 0;
    private long pausedAccumulatedMs = 0;

    private long currentSteps = 0;
    private float currentCalories = 0;
    private int hrSum = 0;
    private int hrCount = 0;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isWorkoutRunning && !isWorkoutPaused) {
                long now = System.currentTimeMillis();
                long elapsed = now - workoutStartTimeMs - pausedAccumulatedMs;
                updateTimerText(elapsed);

                if (bluetoothGatt != null && stepsCharacteristic != null) {
                    bluetoothGatt.readCharacteristic(stepsCharacteristic);
                }

                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private final List<WorkoutSession> sessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_workout);

        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvTimer = findViewById(R.id.tvTimer);
        tvSteps = findViewById(R.id.tvStepsWorkout);
        tvHrWorkout = findViewById(R.id.tvHrWorkout);
        tvCalories = findViewById(R.id.tvCalories);
        tvWorkoutStatus = findViewById(R.id.tvWorkoutStatus);

        btnStart = findViewById(R.id.btnStartWorkout);
        btnPause = findViewById(R.id.btnPauseWorkout);
        btnStop = findViewById(R.id.btnStopWorkout);

        logContainer = findViewById(R.id.logContainer);

        String deviceAddress = getIntent().getStringExtra("device_address");
        String deviceName = getIntent().getStringExtra("device_name");

        if (deviceAddress == null || deviceName == null) {
            Toast.makeText(this, "Missing device info", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDeviceName.setText("Device: " + deviceName + " (" + deviceAddress + ")");

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        connectToDevice(device);

        btnStart.setOnClickListener(v -> {
            if (!isWorkoutRunning) {
                startWorkout();
            } else if (isWorkoutPaused) {
                resumeWorkout();
            }
        });

        btnPause.setOnClickListener(v -> {
            if (isWorkoutRunning && !isWorkoutPaused) {
                pauseWorkout();
            }
        });

        btnStop.setOnClickListener(v -> {
            if (isWorkoutRunning) {
                stopWorkout();
            }
        });
    }

    private void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth connect permission missing", Toast.LENGTH_SHORT).show();
            return;
        }

        tvWorkoutStatus.setText("Connecting...");

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
                runOnUiThread(() -> tvWorkoutStatus.setText("Connected. Discovering services..."));
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    tvWorkoutStatus.setText("Disconnected");
                    tvHrWorkout.setText("--");
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
                runOnUiThread(() -> tvWorkoutStatus.setText("HR service not found"));
                return;
            }

            hrCharacteristic = service.getCharacteristic(HR_CHAR_UUID);
            controlCharacteristic = service.getCharacteristic(CONTROL_CHAR_UUID);
            stepsCharacteristic = service.getCharacteristic(STEPS_CHAR_UUID);

            if (hrCharacteristic == null || controlCharacteristic == null || stepsCharacteristic == null) {
                runOnUiThread(() -> tvWorkoutStatus.setText("Characteristics missing"));
                return;
            }

            enableHeartRateNotifications(gatt);

            runOnUiThread(() -> tvWorkoutStatus.setText("Ready"));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (HR_CHAR_UUID.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                int bpm = 0;
                if (data != null && data.length > 0) {
                    bpm = data[0] & 0xFF;
                }
                final int finalBpm = bpm;

                hrSum += bpm;
                hrCount++;

                runOnUiThread(() -> tvHrWorkout.setText(String.valueOf(finalBpm)));
            } else if (STEPS_CHAR_UUID.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                long steps = 0;
                if (data != null && data.length >= 4) {
                    steps = ((long) (data[0] & 0xFF)) |
                            ((long) (data[1] & 0xFF) << 8) |
                            ((long) (data[2] & 0xFF) << 16) |
                            ((long) (data[3] & 0xFF) << 24);
                }
                currentSteps = steps;
                currentCalories = currentSteps * CALORIES_PER_STEP;

                final long finalSteps = steps;
                final float finalCalories = currentCalories;

                runOnUiThread(() -> {
                    tvSteps.setText(String.valueOf(finalSteps));
                    tvCalories.setText(String.format(Locale.getDefault(), "%.1f kcal", finalCalories));
                });
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (STEPS_CHAR_UUID.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                long steps = 0;
                if (data != null && data.length >= 4) {
                    steps = ((long) (data[0] & 0xFF)) |
                            ((long) (data[1] & 0xFF) << 8) |
                            ((long) (data[2] & 0xFF) << 16) |
                            ((long) (data[3] & 0xFF) << 24);
                }
                currentSteps = steps;
                currentCalories = currentSteps * CALORIES_PER_STEP;

                final long finalSteps = steps;
                final float finalCalories = currentCalories;

                runOnUiThread(() -> {
                    tvSteps.setText(String.valueOf(finalSteps));
                    tvCalories.setText(String.format(Locale.getDefault(), "%.1f kcal", finalCalories));
                });
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

    private void startWorkout() {
        if (bluetoothGatt == null || controlCharacteristic == null) {
            Toast.makeText(this, "Not connected to device", Toast.LENGTH_SHORT).show();
            return;
        }

        isWorkoutRunning = true;
        isWorkoutPaused = false;
        workoutStartTimeMs = System.currentTimeMillis();
        pausedAccumulatedMs = 0;
        hrSum = 0;
        hrCount = 0;
        currentSteps = 0;
        currentCalories = 0;

        tvWorkoutStatus.setText("Workout running");
        btnStart.setText("Resume");
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);

        sendControlCommand(CMD_START_IMU);

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void pauseWorkout() {
        isWorkoutPaused = true;
        pauseStartTimeMs = System.currentTimeMillis();
        tvWorkoutStatus.setText("Paused");
        btnPause.setEnabled(false);

        sendControlCommand(CMD_STOP_IMU);
    }

    private void resumeWorkout() {
        isWorkoutPaused = false;
        long now = System.currentTimeMillis();
        pausedAccumulatedMs += (now - pauseStartTimeMs);

        tvWorkoutStatus.setText("Workout running");
        btnPause.setEnabled(true);

        sendControlCommand(CMD_START_IMU);
    }

    private void stopWorkout() {
        isWorkoutRunning = false;
        isWorkoutPaused = false;

        timerHandler.removeCallbacks(timerRunnable);

        long now = System.currentTimeMillis();
        long durationMs = now - workoutStartTimeMs - pausedAccumulatedMs;

        sendControlCommand(CMD_STOP_IMU);

        float avgHr = (hrCount > 0) ? (float) hrSum / hrCount : 0f;
        WorkoutSession session = new WorkoutSession(durationMs, currentSteps, avgHr, currentCalories);
        sessions.add(session);
        renderWorkoutLogs();

        tvWorkoutStatus.setText("Workout stopped");
        btnStart.setText("Start");
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
    }

    private void renderWorkoutLogs() {
        logContainer.removeAllViews();

        for (int i = 0; i < sessions.size(); i++) {
            WorkoutSession s = sessions.get(i);

            WorkoutSessionView row = new WorkoutSessionView(this);
            final int index = i;
            row.bind(s, index, new WorkoutSessionView.OnDeleteListener() {
                @Override
                public void onDelete(int indexToDelete) {
                    sessions.remove(indexToDelete);
                    renderWorkoutLogs();
                }
            });
            logContainer.addView(row);
        }
    }

    private void updateTimerText(long elapsedMs) {
        long totalSeconds = elapsedMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        String text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimer.setText(text);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
