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
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.data.DatabaseProvider;
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
//    private static final UUID HR_SERVICE_UUID =
//            UUID.fromString("12345678-1234-5678-1234-56789abcdef0");  // Custom Heart Rate–like service
    private static final UUID HR_SERVICE_UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"); // Standard BLE Heart Rate Service
    private static final UUID HR_CHAR_UUID =
            UUID.fromString("12345678-1234-5678-1234-56789abcdef1");  // Characteristic that holds BPM data
    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");  // Standard Client Characteristic Config (CCCD)

    // Request code for runtime permission dialog
    private static final int REQ_PERMISSIONS = 42;


    //Bus BroadcastReceiver
    private final BroadcastReceiver bus = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Bus.ACTION_STATE.equals(action)) {
                String state = intent.getStringExtra(Bus.EXTRA_STATE);

                // ---------------- CHANGED ----------------
                // Map string state from BLE service to ConnectionState
                if ("CONNECTED".equals(state)) {
                    vm.setState(ConnectionState.CONNECTED);
                } /*else if ("CONNECTING".equals(state)) {
                    vm.setState(ConnectionState.CONNECTING);
                } */else if ("DISCONNECTED".equals(state)) {
                    vm.setState(ConnectionState.DISCONNECTED);
                }

            } else if (Bus.ACTION_HR_UPDATE.equals(action)) {
                int bpm = intent.getIntExtra(Bus.EXTRA_BPM, -1);
                tvBpm.setText(getString(R.string.hr_bpm, bpm));
                addPoint(bpm);

            } else if (Bus.ACTION_ERROR.equals(action)) {
                Snackbar.make(requireView(), intent.getStringExtra(Bus.EXTRA_ERROR), Snackbar.LENGTH_LONG).show();
            }
        }
    };

    @Nullable @Override
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

        LineDataSet set = new LineDataSet(null, "Heart Rate");
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        LineData data = new LineData(set);
        chart.setData(data);
        chart.invalidate();

        vm = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        prefs = new AppPrefs(requireContext());

        //Observer
        vm.getState().observe(getViewLifecycleOwner(), this::applyState);
        // Restore flags + last device

        vm.setAutoReconnectEnabled(prefs.isAutoReconnect());
        String lastName = prefs.getLastDeviceName();
        String lastAddr = prefs.getLastDeviceAddr();
        if (!TextUtils.isEmpty(lastName)) {
            vm.setDevice(lastName, lastAddr);
            chipDevice.setText(lastName);
        }



        vm.getState().observe(getViewLifecycleOwner(), s -> {
            applyState(s);
            btnStartWorkout.setEnabled(s == ConnectionState.CONNECTED);
        });

        vm.getBpm().observe(getViewLifecycleOwner(),
                bpm -> tvBpm.setText(bpm == null ? "-- bpm" : getString(R.string.hr_bpm, bpm)));

        // Scan result


        // Connect
        btnConnect.setOnClickListener(click -> {
            vm.setState(ConnectionState.CONNECTING);  // Update state immediately
            startBleFlow();

        });

        // Disconnect (confirm)
        btnDisconnect.setOnClickListener(vw -> {
            requireContext().stopService(new Intent(requireContext(), BleService.class));
            vm.setState(ConnectionState.DISCONNECTED);

            // RESET CHART
            resetChart();            // Reset chart safely
            sampleIdx = 0;
            tvBpm.setText("-- bpm");

        });



        // Fake sensor toggle — also mark CONNECTED so Start is enabled
        switchFake.setChecked(vm.isFakeSensorEnabled());
        switchFake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vm.setFakeSensorEnabled(isChecked);
            if (isChecked) {
                if (vm.getState().getValue() != ConnectionState.CONNECTED) {
                    vm.setState(ConnectionState.CONNECTED);
                }
                SnackbarUtil.show(requireView(), getString(R.string.fake_on));
            } else {
                // When turning off, keep current connection state; BPM will clear to "--"
                SnackbarUtil.show(requireView(), getString(R.string.fake_off));
            }
        });

        // Start Workout (guard)
        btnStartWorkout.setOnClickListener(vw -> {
            ConnectionState s = vm.getState().getValue();
            if (s != ConnectionState.CONNECTED) {
                SnackbarUtil.show(requireView(), getString(R.string.need_connection_snackbar));
                return;
            }
            String device = vm.getDeviceName().getValue();
            long startedAt = System.currentTimeMillis();

            Bundle args = new Bundle();
            args.putString(WorkoutFragment.ARG_DEVICE_NAME, device == null ? "" : device);
            args.putLong(WorkoutFragment.ARG_STARTED_AT, startedAt);

                // Start workout lifecycle (persisted on background executor)
                try {
                    // create workout and wait for id so subsequent sessions attach reliably
                    long wid = DatabaseProvider.get(requireContext()).getSessionController().startWorkoutSync(startedAt);
                    if (wid > 0) {
                        // store current workout id is already handled inside controller; optionally pass to fragment
                    }
                } catch (Exception ignored) {}

                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_dashboard_to_workout, args);
        });

        if (vm.getState().getValue() == null) vm.setState(ConnectionState.DISCONNECTED);
        return v;
    }

    private void applyState(ConnectionState s) {
        switch (s) {
            case DISCONNECTED:
                btnConnect.setEnabled(true);
                btnDisconnect.setEnabled(false); // disconnect disabled when not connected
                tvState.setText("Disconnected");
                break;

           /*// case CONNECTING:
                btnConnect.setEnabled(false);   // prevent duplicate connects
                btnDisconnect.setEnabled(false);
                tvState.setText("Connecting…");
                break;*/

            case CONNECTED:
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);  // ENABLE disconnect when connected
                tvState.setText("Connected");
                break;
        }
    }

    private void startBleFlow() {
        requireContext().startService(
                new Intent(requireContext(), BleService.class)
                        .setAction(BleService.ACTION_START)
        );
        startActivity(new Intent(requireContext(), DeviceScanActivity.class));
    }

    private void resetChart() {
        LineData data = chart.getData();
        if (data != null) {
            LineDataSet ds = (LineDataSet) data.getDataSetByIndex(0);
            if (ds != null) ds.clear();
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    private void addPoint(int bpm) {
        LineData data = chart.getData();
        if (data == null) return;

        LineDataSet ds = (LineDataSet) data.getDataSetByIndex(0);
        if (ds == null) return;

        ds.addEntry(new Entry(sampleIdx++, bpm));
        while (ds.getEntryCount() > 60) ds.removeFirst();

        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.moveViewToX(sampleIdx);
        chart.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(Bus.ACTION_STATE);
        f.addAction(Bus.ACTION_HR_UPDATE);
        f.addAction(Bus.ACTION_ERROR);
        requireContext().registerReceiver(bus, f, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(bus);
    }
}


