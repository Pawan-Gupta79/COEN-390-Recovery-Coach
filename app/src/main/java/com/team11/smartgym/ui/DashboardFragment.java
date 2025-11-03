package com.team11.smartgym.ui;

import android.app.Activity;
import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.team11.smartgym.R;
import com.team11.smartgym.data.AppPrefs;
import com.team11.smartgym.model.ConnectionState;
import com.team11.smartgym.ui.common.SnackbarUtil;

public class DashboardFragment extends Fragment {

    private Chip chipDevice;
    private LineChart chart;
    private TextView tvBpm, tvState;
    private MaterialButton btnConnect, btnDisconnect, btnStartWorkout;
    private MaterialSwitch switchFake;

    private DashboardViewModel vm;
    private AppPrefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> scanLauncher;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        chipDevice      = v.findViewById(R.id.chipDevice);
        chart           = v.findViewById(R.id.chart);
        tvBpm           = v.findViewById(R.id.tvBpm);
        tvState         = v.findViewById(R.id.tvState);
        btnConnect      = v.findViewById(R.id.btnConnect);
        btnDisconnect   = v.findViewById(R.id.btnDisconnect);
        btnStartWorkout = v.findViewById(R.id.btnStartWorkout);
        switchFake      = v.findViewById(R.id.switchFake);

        vm    = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        prefs = new AppPrefs(requireContext());

        // Restore flags + last device
        vm.setAutoReconnectEnabled(prefs.isAutoReconnect());
        String lastName = prefs.getLastDeviceName();
        String lastAddr = prefs.getLastDeviceAddr();
        if (!TextUtils.isEmpty(lastName)) {
            vm.setDevice(lastName, lastAddr);
            chipDevice.setText(lastName);
        }

        // Observers
        vm.getDeviceName().observe(getViewLifecycleOwner(), name ->
                chipDevice.setText(TextUtils.isEmpty(name)
                        ? getString(R.string.state_disconnected)
                        : name));

        vm.getState().observe(getViewLifecycleOwner(), s -> {
            applyState(s);
            btnStartWorkout.setEnabled(s == ConnectionState.CONNECTED);
        });

        vm.getBpm().observe(getViewLifecycleOwner(),
                bpm -> tvBpm.setText(bpm == null ? "-- bpm" : getString(R.string.hr_bpm, bpm)));

        // Scan result
        scanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String name = data.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_NAME);
                        String addr = data.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_ADDR);
                        if (!TextUtils.isEmpty(name)) {
                            vm.setDevice(name, addr);
                            prefs.setLastDevice(name, addr);
                            vm.setState(ConnectionState.CONNECTING);
                            handler.postDelayed(() -> {
                                vm.setState(ConnectionState.CONNECTED);
                                SnackbarUtil.show(requireView(), getString(R.string.connected_snackbar));
                            }, 700);
                        }
                    }
                });

        // Connect
        btnConnect.setOnClickListener(click -> {
            boolean auto = vm.isAutoReconnectEnabled();
            String saved = vm.getDeviceName().getValue();
            if (auto && !TextUtils.isEmpty(saved)) {
                vm.setState(ConnectionState.CONNECTING);
                handler.postDelayed(() -> {
                    vm.setState(ConnectionState.CONNECTED);
                    SnackbarUtil.show(requireView(), getString(R.string.connected_snackbar));
                }, 650);
            } else {
                scanLauncher.launch(new Intent(requireContext(), DeviceScanActivity.class));
            }
        });

        // Disconnect (confirm)
        btnDisconnect.setOnClickListener(vw -> new MaterialAlertDialogBuilder(requireContext())
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setTitle(R.string.confirm_disconnect_title)
                .setMessage(R.string.confirm_disconnect_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.disconnect, (d, w) -> {
                    vm.stopFakeSensor();
                    vm.setState(ConnectionState.DISCONNECTED);
                    SnackbarUtil.show(requireView(), getString(R.string.disconnected_snackbar));
                })
                .show());

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

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_dashboard_to_workout, args);
        });

        if (vm.getState().getValue() == null) vm.setState(ConnectionState.DISCONNECTED);
        return v;
    }

    private void applyState(ConnectionState s) {
        switch (s) {
            case DISCONNECTED:
                chipDevice.setChipIconResource(android.R.drawable.stat_sys_data_bluetooth);
                chipDevice.setText(getString(R.string.state_disconnected));
                tvState.setText(getString(R.string.state_disconnected));
                btnConnect.setEnabled(true);
                btnDisconnect.setEnabled(false);
                break;
            case CONNECTING:
                chipDevice.setChipIconResource(android.R.drawable.stat_sys_upload);
                tvState.setText(getString(R.string.state_connecting));
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(false);
                break;
            case CONNECTED:
                chipDevice.setChipIconResource(android.R.drawable.presence_online);
                String dn = vm.getDeviceName().getValue();
                chipDevice.setText(TextUtils.isEmpty(dn) ? getString(R.string.state_connected) : dn);
                tvState.setText(getString(R.string.state_connected));
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);
                break;
            case RECONNECTING:
                chipDevice.setChipIconResource(android.R.drawable.stat_notify_sync);
                tvState.setText(getString(R.string.state_reconnecting));
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(false);
                break;
        }
    }
}
