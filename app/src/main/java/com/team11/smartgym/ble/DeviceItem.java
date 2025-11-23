package com.team11.smartgym.ble;

import android.bluetooth.BluetoothDevice;

/**
 * Simple model class representing a discovered BLE device.
 * Holds a user-friendly name, MAC address, and the raw BluetoothDevice object.
 */
public class DeviceItem {

    // Display name of the BLE device (may be advertised or resolved)
    public String name;

    // Unique MAC identifier for the device
    public String address;

    // Actual BluetoothDevice instance used for connecting
    public BluetoothDevice device;

    /**
     * Creates a wrapper object for a scanned BLE device.
     *
     * @param name    resolved device name or "(Unknown)"
     * @param address device MAC address
     * @param device  raw BluetoothDevice object
     */
    public DeviceItem(String name, String address, BluetoothDevice device) {
        this.name = name;
        this.address = address;
        this.device = device;
    }

    /**
     * String representation used in UI lists (name + address).
     */
    @Override
    public String toString() {
        return name + " (" + address + ")";
    }
}
