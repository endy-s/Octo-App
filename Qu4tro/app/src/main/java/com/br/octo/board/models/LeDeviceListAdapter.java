package com.br.octo.board.models;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.br.octo.board.R;

import java.util.ArrayList;

/**
 * Created by endysilveira on 16/07/17.
 */

// Adapter for holding devices found through scanning.
public class LeDeviceListAdapter extends ArrayAdapter {
    private Context context;
    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;

    public LeDeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
        super(context, R.layout.device_name, devices);

        this.mLeDevices = devices;
        this.context = context;
    }

    class LeDeviceViewHolder {
        TextView deviceAddress;
        TextView deviceName;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LeDeviceViewHolder LeDeviceItem;
        // General ListView optimization code.

        if (mInflator == null) {
            mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        if (view == null) {
            view = mInflator.inflate(R.layout.device_name, null);

            LeDeviceItem = new LeDeviceViewHolder();

            LeDeviceItem.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            LeDeviceItem.deviceName = (TextView) view.findViewById(R.id.device_name);

            view.setTag(LeDeviceItem);
        } else {
            LeDeviceItem = (LeDeviceViewHolder) view.getTag();
        }

        BluetoothDevice device = mLeDevices.get(i);

        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            LeDeviceItem.deviceName.setText(deviceName);
        }

        LeDeviceItem.deviceAddress.setText(device.getAddress());

        return view;
    }

    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device)) mLeDevices.add(device);
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


}
