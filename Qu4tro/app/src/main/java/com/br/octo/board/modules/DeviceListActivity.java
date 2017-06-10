package com.br.octo.board.modules;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
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
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.modules.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;

public class DeviceListActivity extends BaseActivity implements BluetoothHelper.BluetoothCallback, ProgressDialog.OnCancelListener {
    // Tag for Log
    private static final String TAG = "DeviceListActivity";

    private LeDeviceListAdapter mLeDeviceListAdapter;

    ArrayList<BluetoothDevice> mDevices = new ArrayList<>();;
    private BluetoothLeScanner mBluetoothLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    BluetoothHelper btHelper;

    ProgressDialog pd;


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
        setResult(RESULT_CANCELED);

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this, mDevices);
        mLeDeviceListAdapter.clear();
        bleDevicesListView.setAdapter(mLeDeviceListAdapter);

        // Get the local Bluetooth adapter
        mBluetoothLEScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<>();

        btHelper = BluetoothHelper.getInstance();
        btHelper.setCallback(this);

        scanLeDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLEScanner != null) {
            mBluetoothLEScanner.stopScan(mScanCallback);
        }
    }

    @OnItemClick(R.id.ble_devices)
    protected void onDeviceClicked(AdapterView<?> adapter, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);

        if (device == null) return;

        if (mBluetoothLEScanner != null) {
            mBluetoothLEScanner.stopScan(mScanCallback);
        }

        btHelper.connectToDevice(this, device);

        pd = ProgressDialog.show(this, "Conectando", "Validando dispositivo...", true, true, this);
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
        //setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // If we're already discovering, stop it
        if (mBluetoothLEScanner != null) {
            // Request discover from BluetoothAdapter
            mBluetoothLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends ArrayAdapter {
        private Context context;
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, R.layout.device_name, devices);

            this.mLeDevices = devices;
            this.context = context;
        }

        class LeDeviceViewHolder {
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

        void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        BluetoothDevice getDevice(int position) {
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
    private ScanCallback mScanCallback =
            new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            mLeDeviceListAdapter.addDevice(btDevice);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    // endregion

    //region BT Callback

    @Override
    public void onMessageReceived(String message) {
    }

    @Override
    public void onDeviceConnected() {
        pd.dismiss();

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onDeviceDisconnected() {}

    //endregion

    @Override
    public void onCancel(DialogInterface dialog) {
        btHelper.disconnect();
    }
}
