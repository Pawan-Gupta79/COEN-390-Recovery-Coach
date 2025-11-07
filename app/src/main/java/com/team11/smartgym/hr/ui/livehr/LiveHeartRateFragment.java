package com.team11.smartgym.hr.ui.livehr;


        // this is an incomplete file. Functions StartMonitoringData() and StopMonitoringData() need work


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;   // << ANDROIDX Fragment

import com.team11.smartgym.R;
import com.team11.smartgym.hr.data.BleManager;
import com.team11.smartgym.ui.common.SnackbarUtil;

// Make sure we extend the ANDROIDX Fragment
public class LiveHeartRateFragment extends Fragment {

    private TextView tvState;
    private TextView emptyBox;
    private TextView tvBpm;
    private TextView tvUpdated;
    private Button btnStop;
    private Button btnStart;

    private void StartMonitoringData(){
        updateUiForConnectedState();

        //BleManager.what do i need to put in here for it to function?().enableHR

        SnackbarUtil.show(requireView(),"HR monitoring Started");

    }

    private void StopMonitoringData(){
        updateUiForDisconnectedState();
        //BleManager.what do i need to put in here for it to function?().enableHR

        SnackbarUtil.show(requireView(),"HR monitoring Stopped");

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_live_hr, container, false);

        // 2. Initialize UI fields using IDs from fragment_live_hr.xml
        tvState = v.findViewById(R.id.tvState);
        emptyBox = v.findViewById(R.id.emptyBox);
        tvBpm = v.findViewById(R.id.tvBpm);
        tvUpdated = v.findViewById(R.id.tvUpdated);
        btnStart = v.findViewById(R.id.btnStart);
        btnStop = v.findViewById(R.id.btnStop);

        updateUiForDisconnectedState();

        btnStart.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            StartMonitoringData();
                                        }
                                    }
                //maybe will have to define an enable fucntion in some other java file like enableHRdatain(); to get data
                // then write it in somewhere like int BPM = value.getBPM(); and then maybe average it somehow.
        );

        btnStop.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           StopMonitoringData();
                                       }
                                   }
                //maybe will have to define an enable fucntion in some other java file like enableHRdatain(); to stop getting data
                // then write it in somewhere like int BPM = value.getBPM(); and then maybe average it somehow.
        );



        return v;
    }

    // --- UI State Management Methods ---

    /** Sets the UI for a disconnected state (Show Empty Box, Hide BPM) */
    public void updateUiForDisconnectedState() {
        tvState.setText("DISCONNECTED");
        emptyBox.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.VISIBLE);

        tvBpm.setVisibility(View.GONE);
        tvUpdated.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
    }

    /** Sets the UI for a connected state (Hide Empty Box, Show BPM) */
    public void updateUiForConnectedState() {
        tvState.setText("CONNECTED");
        emptyBox.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);

        tvBpm.setVisibility(View.VISIBLE);
        tvUpdated.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.VISIBLE);
    }
}
