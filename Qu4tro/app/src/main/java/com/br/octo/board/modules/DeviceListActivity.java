package com.br.octo.board.modules;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.models.LeDeviceListAdapter;
import com.br.octo.board.modules.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;

public class DeviceListActivity extends BaseActivity implements BluetoothHelper.BluetoothCallback, ProgressDialog.OnCancelListener {

    private LeDeviceListAdapter mLeDeviceListAdapter;

    ArrayList<BluetoothDevice> mDevices = new ArrayList<>();

    private BluetoothLeScanner mBluetoothLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    BluetoothHelper btHelper;

    ProgressDialog connectingProgressDialogs;
    private Handler mHandler;
    private static final int SCAN_PERIOD = 10000;

    @BindView(R.id.button_scan)
    Button scanButton;

    @BindView(R.id.ble_devices)
    ListView bleDevicesListView;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(getWindow().FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);
        ButterKnife.bind(this);

        setTitle(R.string.select_device);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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

        mHandler = new Handler();
        scanLeDevice(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLEScanner.stopScan(mScanCallback);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnItemClick(R.id.ble_devices)
    protected void onDeviceClicked(AdapterView<?> adapter, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);

        if (device == null) return;

        mBluetoothLEScanner.stopScan(mScanCallback);

        btHelper.connectToDevice(this, device);

        connectingProgressDialogs = ProgressDialog.show(this, getResources().getString(R.string.dialog_connecting_title) + " " + device.getName(),
                getResources().getString(R.string.dialog_connecting_message), true, true, this);
    }

    /**
     * Start device discover with the BluetoothAdapter when click at the button
     */
    @OnClick(R.id.button_scan)
    public void scanClicked() {
        scanLeDevice(true);
    }

    public void scanLeDevice(boolean toScan) {
        if (mBluetoothAdapter.isEnabled()) {
            scanButton.setEnabled(!toScan);

            if (toScan) {
                mLeDeviceListAdapter.clear();

                setTitle(R.string.scanning);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevice(false);
                    }
                }, SCAN_PERIOD);

                mBluetoothLEScanner.startScan(filters, settings, mScanCallback);
            } else {
                setTitle(R.string.select_device);

                mBluetoothLEScanner.stopScan(mScanCallback);
            }
        }
    }

    //endregion

    //region BT Callback

    // Device scan callback.
    private ScanCallback mScanCallback =
            new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    Log.i("callbackType", String.valueOf(callbackType));
                    Log.i("result", result.toString());
                    BluetoothDevice btDevice = result.getDevice();

                    if (btDevice.getName() == null) {
                        return;
                    }

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

    @Override
    public void onMessageReceived(String message) {
    }

    @Override
    public void onDeviceConnected() {
        connectingProgressDialogs.dismiss();

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onDeviceDisconnected() {
        connectingProgressDialogs.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), getString(R.string.bt_connection_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //endregion

    @Override
    public void onCancel(DialogInterface dialog) {
        btHelper.disconnect();
    }
}
