package com.team11.smartgym;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.team11.smartgym.model.ConnectionState;
import com.team11.smartgym.ui.DashboardViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DS-05.3 - Reconnect integration tests at ViewModel level (instrumented).
 *
 * We run all ViewModel interactions on the Android main thread using
 * InstrumentationRegistry.getInstrumentation().runOnMainSync(...)
 * so that LiveData.setValue() and Handler(Looper.getMainLooper())
 * are called on the UI thread.
 */
@RunWith(AndroidJUnit4.class)
public class ReconnectViewModelTest {

    @Test
    public void initialState_isDisconnected() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            DashboardViewModel vm = new DashboardViewModel();
            assertEquals(ConnectionState.DISCONNECTED, vm.getState().getValue());
        });
    }

    @Test
    public void stateTransition_connectingThenConnectedThenDisconnected() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            DashboardViewModel vm = new DashboardViewModel();

            vm.setState(ConnectionState.CONNECTING);
            assertEquals(ConnectionState.CONNECTING, vm.getState().getValue());

            vm.setState(ConnectionState.CONNECTED);
            assertEquals(ConnectionState.CONNECTED, vm.getState().getValue());

            vm.setState(ConnectionState.DISCONNECTED);
            assertEquals(ConnectionState.DISCONNECTED, vm.getState().getValue());
        });
    }

    @Test
    public void fakeSensorFlag_survivesStateChanges() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            DashboardViewModel vm = new DashboardViewModel();

            // Turn fake sensor ON.
            vm.setFakeSensorEnabled(true);
            assertTrue(vm.isFakeSensorEnabled());

            // Simulate some noisy state changes from BLE layer.
            vm.setState(ConnectionState.CONNECTING);
            vm.setState(ConnectionState.RECONNECTING);
            vm.setState(ConnectionState.DISCONNECTED);

            // The fake sensor flag should still be ON.
            assertTrue(vm.isFakeSensorEnabled());

            // Turn fake sensor OFF.
            vm.setFakeSensorEnabled(false);
            assertFalse(vm.isFakeSensorEnabled());
        });
    }
}
