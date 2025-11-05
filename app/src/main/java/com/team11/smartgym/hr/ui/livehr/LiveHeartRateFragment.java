package com.team11.smartgym.hr.ui.livehr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.team11.smartgym.R;
import com.team11.smartgym.hr.domain.ConnectionState;
import com.team11.smartgym.hr.util.UiError;

public class LiveHeartRateFragment extends Fragment {

    private HeartRateViewModel vm;
    private TextView tvBpm, tvUpdated, tvState;
    private View emptyBox;
    private Button btnStart, btnStop;

    // Inline banner (sticky)
    private View banner;
    private TextView bannerMsg;
    private Button bannerRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_hr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tvBpm     = v.findViewById(R.id.tvBpm);
        tvUpdated = v.findViewById(R.id.tvUpdated);
        tvState   = v.findViewById(R.id.tvState);
        emptyBox  = v.findViewById(R.id.emptyBox);
        btnStart  = v.findViewById(R.id.btnStart);
        btnStop   = v.findViewById(R.id.btnStop);

        banner      = v.findViewById(R.id.retryBanner);
        bannerMsg   = v.findViewById(R.id.retryMsg);
        bannerRetry = v.findViewById(R.id.btnRetry);

        // Ensure banner sits on top of everything and can receive clicks
        banner.bringToFront();
        banner.setClickable(true);
        banner.setFocusable(true);

        vm = new ViewModelProvider(this).get(HeartRateViewModel.class);

        btnStart.setOnClickListener(_v -> vm.start());
        btnStop.setOnClickListener(_v -> vm.stop());

        // 1) Button click → retry
        bannerRetry.setOnClickListener(_v -> {
            Toast.makeText(requireContext(), "Retry pressed", Toast.LENGTH_SHORT).show();
            Snackbar.make(requireView(), "Retry pressed", Snackbar.LENGTH_SHORT).show();
            vm.retryNow();
            // Keep banner visible until data resumes so user sees it's trying
        });

        // 2) Whole banner is clickable too (fallback if emulator covers button)
        banner.setOnClickListener(_v -> {
            Toast.makeText(requireContext(), "Retry (banner) pressed", Toast.LENGTH_SHORT).show();
            Snackbar.make(requireView(), "Retry (banner) pressed", Snackbar.LENGTH_SHORT).show();
            vm.retryNow();
        });

        vm.bpm().observe(getViewLifecycleOwner(), bpm -> {
            boolean hasData = (bpm != null);
            tvBpm.setText(hasData ? String.valueOf(bpm) : "--");
            emptyBox.setVisibility(hasData ? View.GONE : View.VISIBLE);
            if (hasData) hideBanner(); // data flowing again → hide banner
        });

        vm.lastUpdated().observe(getViewLifecycleOwner(), ts ->
                tvUpdated.setText(ts == null ? "" : getString(R.string.last_updated, ts)));

        vm.connection().observe(getViewLifecycleOwner(), state -> {
            tvState.setText(state == null ? "" : state.name());
            boolean canStart = (state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR);
            btnStart.setEnabled(canStart);
            btnStop.setEnabled(state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING);
        });

        // Keep snackbars only for short info, not for errors
        vm.uiSnackbar().observe(getViewLifecycleOwner(), msg -> {
            if (getView() != null && msg != null && !msg.isEmpty()) {
                Snackbar.make(getView(), msg, Snackbar.LENGTH_SHORT).show();
            }
        });

        // Errors → sticky banner with Retry (no snackbar for errors)
        vm.uiError().observe(getViewLifecycleOwner(), this::handleUiError);
    }

    private void handleUiError(UiError err) {
        if (err == null) return;

        if (err.isBlocking) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setMessage(err.message)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } else {
            showBanner(
                    (err.message == null || err.message.isEmpty()) ? "Device not responding." : err.message,
                    (err.action == null || err.action.isEmpty()) ? getString(R.string.retry) : err.action
            );
        }
    }

    private void showBanner(String message, String actionText) {
        bannerMsg.setText(message);
        bannerRetry.setText(actionText);
        bannerRetry.setEnabled(true);
        banner.setVisibility(View.VISIBLE);
        banner.bringToFront();
    }

    private void hideBanner() {
        if (banner != null && banner.getVisibility() == View.VISIBLE) {
            banner.setVisibility(View.GONE);
        }
    }

    @Override public void onResume() {
        super.onResume();
        vm.setForeground(true);
        vm.start();
    }

    @Override public void onPause() {
        super.onPause();
        vm.setForeground(false);
        vm.stop();
        hideBanner();
    }
}
