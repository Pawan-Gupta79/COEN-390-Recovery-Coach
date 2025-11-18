package com.team11.smartgym.ble;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter used to display scanned BLE devices in a simple two-line list.
 * Shows device name on the first line and MAC address on the second line.
 */
public class DeviceListAdapter extends ArrayAdapter<DeviceItem> {

    /**
     * Creates an adapter using Android’s built-in two-line layout.
     *
     * @param context  current Activity/Context
     * @param devices  list of DeviceItem objects to display
     */
    public DeviceListAdapter(Context context, List<DeviceItem> devices) {
        // Use built-in simple_list_item_2 and bind the first text label to text1
        super(context, android.R.layout.simple_list_item_2, android.R.id.text1, devices);
    }

    /**
     * Populates the two lines of the list row with device name + address.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate/reuse default two-line layout
        View v = super.getView(position, convertView, parent);

        // Get references to the two built-in text fields
        TextView text1 = v.findViewById(android.R.id.text1);
        TextView text2 = v.findViewById(android.R.id.text2);

        // Retrieve corresponding DeviceItem for this row
        DeviceItem item = getItem(position);
        if (item != null) {
            text1.setText(item.name);     // First line: device name
            text2.setText(item.address);  // Second line: MAC address
        }

        return v;
    }
}
