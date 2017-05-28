package com.br.octo.board.modules;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.br.octo.board.R;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.main.MainActivity;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;

public class DeviceListActivity extends BaseActivity {
    //Change to a DialogFragment in the future


    // Tag for Log
    private static final String TAG = "DeviceListActivity";

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private LeDeviceListAdapter mLeDeviceListAdapter;

    ArrayList<BluetoothDevice> mDevices = new ArrayList<>();;
    private BluetoothAdapter mBluetoothAdapter;


    @BindView(R.id.button_scan)
    Button scanButton;

    @BindView(R.id.ble_devices)
    ListView bleDevicesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // "Back" Arrow
        setTitle(R.string.select_device);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this, mDevices);
        mLeDeviceListAdapter.clear();
        bleDevicesListView.setAdapter(mLeDeviceListAdapter);

        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @OnItemClick(R.id.ble_devices)
    protected void onDeviceClicked(AdapterView<?> adapter, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);

        if (device == null) return;

        final Intent intent = new Intent();

        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Start device discover with the BluetoothAdapter when click at the button
     */
    @OnClick(R.id.button_scan)
    public void scanLeDevice() {
        Log.d(TAG, "scanLeDevice()");
        scanButton.setEnabled(false);

        mLeDeviceListAdapter.clear();

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // If we're already discovering, stop it
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }

            // Request discover from BluetoothAdapter
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }




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

        public class LeDeviceViewHolder {
            TextView     deviceAddress;
            TextView     deviceName;
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
            } else {
                LeDeviceItem.deviceName.setText(R.string.unknown_device);
            }

            LeDeviceItem.deviceAddress.setText(device.getAddress());

            return view;
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
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

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };
}
