package com.team11.smartgym.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
import com.team11.smartgym.ble.BleService;
import com.team11.smartgym.data.AppDb;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.data.SessionRepository;
import com.team11.smartgym.model.ConnectionState;
import com.team11.smartgym.shared.Bus;
import com.team11.smartgym.ui.common.SnackbarUtil;

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

    private final BroadcastReceiver bus = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Bus.ACTION_STATE.equals(action)) {
                String state = intent.getStringExtra(Bus.EXTRA_STATE);
                if (TextUtils.isEmpty(state)) {
                    return;
                }

                // If fake sensor is driving the UI, ignore BLE state noise.
                if (vm != null && vm.isFakeSensorEnabled()) {
                    return;
                }

                // DS-05: map BLE state strings to ConnectionState + UI status
                if ("Connected".equals(state)) {
                    vm.setState(ConnectionState.CONNECTED);
                    tvState.setText("Connected");

                } else if ("Disconnected".equals(state)) {
                    vm.setState(ConnectionState.DISCONNECTED);
                    tvState.setText("Disconnected");

                } else if (state.startsWith("Connecting") || state.startsWith("Discovering")) {
                    vm.setState(ConnectionState.CONNECTING);
                    tvState.setText("Connecting…");

                } else if (state.startsWith("Reconnecting")) {
                    vm.setState(ConnectionState.RECONNECTING);
                    // Example: "Reconnecting… (2)"
                    tvState.setText(state);

                } else if ("ReconnectFailed".equals(state) || "ReconnectFailedNoDevice".equals(state)) {
                    vm.setState(ConnectionState.DISCONNECTED);
                    tvState.setText("Reconnect failed");
                    // Non-blocking retry prompt
                    SnackbarUtil.show(
                            requireView(),
                            "Reconnect failed. Tap Connect to retry."
                    );
                }

            } else if (Bus.ACTION_HR_UPDATE.equals(action)) {
                int bpm = intent.getIntExtra(Bus.EXTRA_BPM, -1);
                tvBpm.setText(getString(R.string.hr_bpm, bpm));
                addPoint(bpm);

            } else if (Bus.ACTION_ERROR.equals(action)) {
                Snackbar.make(requireView(),
                        intent.getStringExtra(Bus.EXTRA_ERROR),
                        Snackbar.LENGTH_LONG).show();
            }
        }
    };

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        chipDevice = v.findViewById(R.id.chipDevice);
        chart = v.findViewById(R.id.chart);
        tvBpm = v.findViewById(R.id.tvBpm);
        tvState = v.findViewById(R.id.tvState);
        btnConnect = v.findViewById(R.id.btnConnect);
        btnDisconnect = v.findViewById(R.id.btnDisconnect);
        btnStartWorkout = v.findViewById(R.id.btnStartWorkout);
        switchFake = v.findViewById(R.id.switchFake);

        prefs = new AppPrefs(requireContext());
        vm = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        setupChart();

        // Device name chip
        vm.getDeviceName().observe(getViewLifecycleOwner(), name -> {
            if (TextUtils.isEmpty(name)) {
                chipDevice.setText(getString(R.string.tap_connect_to_begin));
            } else {
                chipDevice.setText(name);
            }
        });

        // Connection state
        vm.getState().observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;
            applyState(s);
            btnStartWorkout.setEnabled(s == ConnectionState.CONNECTED);
        });

        // BPM
        vm.getBpm().observe(getViewLifecycleOwner(),
                bpm -> tvBpm.setText(bpm == null ? "-- bpm" : getString(R.string.hr_bpm, bpm)));

        // Scan result from DeviceScanActivity
        scanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    String deviceName = result.getData().getStringExtra("device_name");
                    String deviceAddr = result.getData().getStringExtra("device_addr");

                    if (!TextUtils.isEmpty(deviceName)) {
                        vm.setDevice(deviceName, deviceAddr);
                    }
                });

        chipDevice.setOnClickListener(click -> {
            Intent i = new Intent(requireContext(), DeviceScanActivity.class);
            scanLauncher.launch(i);
        });

        // Connect
        btnConnect.setOnClickListener(click -> {
            vm.setState(ConnectionState.CONNECTING);  // immediate UI feedback
            startBleFlow();
        });

        // Disconnect (confirm)
        btnDisconnect.setOnClickListener(vw -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_disconnect_title)
                    .setMessage(R.string.confirm_disconnect_msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.disconnect, (d, which) -> {
                        requireContext().stopService(
                                new Intent(requireContext(), BleService.class)
                        );
                        vm.setState(ConnectionState.DISCONNECTED);

                        // Reset chart + BPM
                        resetChart();
                        sampleIdx = 0;
                        tvBpm.setText("-- bpm");
                    })
                    .show();
        });

        // Fake sensor toggle
        switchFake.setChecked(vm.isFakeSensorEnabled());
        switchFake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vm.setFakeSensorEnabled(isChecked);
            if (isChecked) {
                // Force UI into CONNECTED when fake sensor is on
                vm.setState(ConnectionState.CONNECTED);
                SnackbarUtil.show(requireView(), getString(R.string.fake_on));
            } else {
                SnackbarUtil.show(requireView(), getString(R.string.fake_off));
            }
        });

        // Start Workout
        btnStartWorkout.setOnClickListener(vw -> {
            ConnectionState s = vm.getState().getValue();
            if (s != ConnectionState.CONNECTED) {
                SnackbarUtil.show(requireView(),
                        getString(R.string.need_connection_snackbar));
                return;
            }

            String device = vm.getDeviceName().getValue();
            long startedAt = System.currentTimeMillis();

            // DS-06.1: create Session row on Start and get its ID.
            SessionRepository sessionRepo =
                    new SessionRepository(AppDb.get(requireContext()).sessionDao());
            long sessionId = sessionRepo.createSession(startedAt);

            Bundle args = new Bundle();
            args.putString(WorkoutFragment.ARG_DEVICE_NAME,
                    device == null ? "" : device);
            args.putLong(WorkoutFragment.ARG_STARTED_AT, startedAt);
            // Pass sessionId forward so WorkoutFragment can attach samples later (DS-06.2+).
            args.putLong("sessionId", sessionId);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_dashboard_to_workout, args);
        });

        return v;
    }

    private void setupChart() {
        LineDataSet set = new LineDataSet(null, "Heart Rate");
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);

        LineData data = new LineData(set);
        chart.setData(data);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }

    private void startBleFlow() {
        // Start BLE service + go to scan screen
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
        if (ds == null) {
            ds = new LineDataSet(null, "Heart Rate");
            data.addDataSet(ds);
        }

        data.addEntry(new Entry(sampleIdx++, bpm), 0);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void applyState(ConnectionState s) {
        switch (s) {
            case DISCONNECTED:
                btnConnect.setEnabled(true);
                btnDisconnect.setEnabled(false);
                tvState.setText("Disconnected");
                break;

            case CONNECTING:
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(false);
                tvState.setText("Connecting…");
                break;

            case RECONNECTING:
                // For reconnecting, we keep the buttons disabled and let
                // state messages (Reconnecting… (n)) drive tvState text.
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(false);
                break;

            case CONNECTED:
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);
                tvState.setText("Connected");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // When coming back, normalize any transient "stuck" states.
        ConnectionState current = vm.getState().getValue();
        if (current == ConnectionState.RECONNECTING ||
                current == ConnectionState.CONNECTING) {
            vm.setState(ConnectionState.DISCONNECTED);
        }

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

        // DS-05: cancel any pending auto-reconnect when leaving Dashboard
        requireContext().startService(
                new Intent(requireContext(), BleService.class)
                        .setAction(BleService.ACTION_CANCEL_RECONNECT)
        );
    }
}
