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
    private MaterialButton btnConnect, btnDisconnect;
    private MaterialSwitch switchFake;

    private ConnectionViewModel vm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> scanLauncher;
    private AppPrefs prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        chipDevice    = v.findViewById(R.id.chipDevice);
        chart         = v.findViewById(R.id.chart);
        tvBpm         = v.findViewById(R.id.tvBpm);
        tvState       = v.findViewById(R.id.tvState);
        btnConnect    = v.findViewById(R.id.btnConnect);
        btnDisconnect = v.findViewById(R.id.btnDisconnect);
        switchFake    = v.findViewById(R.id.switchFake);

        vm    = new ViewModelProvider(requireActivity()).get(ConnectionViewModel.class);
        prefs = new AppPrefs(requireContext());

        // ----- Load persisted flags & last device into VM
        vm.setAutoReconnectEnabled(prefs.isAutoReconnect());
        String lastName = prefs.getLastDeviceName();
        String lastAddr = prefs.getLastDeviceAddr();
        if (!TextUtils.isEmpty(lastName)) {
            vm.setDevice(lastName, lastAddr);
            chipDevice.setText(lastName);
        }

        // Observe name/state/bpm
        vm.getDeviceName().observe(getViewLifecycleOwner(), name -> {
            chipDevice.setText(TextUtils.isEmpty(name)
                    ? getString(R.string.state_disconnected)
                    : name);
        });
        vm.getState().observe(getViewLifecycleOwner(), this::applyState);
        vm.getBpm().observe(getViewLifecycleOwner(), value ->
                tvBpm.setText(value == null ? "-- bpm" : getString(R.string.hr_bpm, value)));

        // Scan result: save device to prefs and connect
        scanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String name = data.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_NAME);
                        String addr = data.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_ADDR);
                        if (!TextUtils.isEmpty(name)) {
                            vm.setDevice(name, addr);
                            prefs.setLastDevice(name, addr); // persist last device
                            chipDevice.setText(name);

                            vm.setState(ConnectionState.CONNECTING);
                            handler.postDelayed(() -> {
                                vm.setState(ConnectionState.CONNECTED);
                                SnackbarUtil.show(requireView(), getString(R.string.connected_snackbar));
                            }, 800);
                        }
                    }
                });

        // Connect respects Auto-reconnect flag
        btnConnect.setOnClickListener(vw -> {
            boolean auto = vm.isAutoReconnectEnabled();
            String savedName = vm.getDeviceName().getValue();

            if (auto && !TextUtils.isEmpty(savedName)) {
                vm.setState(ConnectionState.CONNECTING);
                handler.postDelayed(() -> {
                    vm.setState(ConnectionState.CONNECTED);
                    SnackbarUtil.show(requireView(), getString(R.string.connected_snackbar));
                }, 700);
            } else {
                scanLauncher.launch(new Intent(requireContext(), DeviceScanActivity.class));
            }
        });

        // Disconnect confirm
        btnDisconnect.setOnClickListener(vw -> new MaterialAlertDialogBuilder(requireContext())
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setTitle(R.string.confirm_disconnect_title)
                .setMessage(R.string.confirm_disconnect_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.disconnect, (d, w) -> {
                    vm.setState(ConnectionState.DISCONNECTED);
                    vm.stopFakeSensor();
                    SnackbarUtil.show(requireView(), getString(R.string.disconnected_snackbar));
                    // We KEEP last device (Option A)
                })
                .show());

        // Fake sensor
        switchFake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                vm.startFakeSensor();
                SnackbarUtil.show(requireView(), getString(R.string.fake_on));
            } else {
                vm.stopFakeSensor();
                SnackbarUtil.show(requireView(), getString(R.string.fake_off));
            }
        });

        if (vm.getState().getValue() == null) vm.setState(ConnectionState.DISCONNECTED);
        return v;
    }

    private void applyState(ConnectionState s) {
        switch (s) {
            case DISCONNECTED:
                chipDevice.setChipIconResource(android.R.drawable.stat_sys_data_bluetooth);
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
