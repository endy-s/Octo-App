package com.br.octo.board.modules.main;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothService;
import com.br.octo.board.api_services.SampleGattAttributes;
import com.br.octo.board.models.QuatroDialogFragment;
import com.br.octo.board.modules.DeviceListActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.settings.LightSettingsActivity;
import com.br.octo.board.modules.settings.LocaleHelper;
import com.br.octo.board.modules.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Endy.
 */
public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    QuatroDialogFragment.QuatroDialogListener {

// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE     = 0;
    private static final int REQUEST_GENERAL_SETTINGS   = 1;
    private static final int REQUEST_LIGHT_SETTINGS     = 2;
    private static final int REQUEST_TRACKING_SCREEN    = 3;
    private static final int REQUEST_ENABLE_BT          = 99;

// Bluetooth

    private BluetoothAdapter mBluetoothAdapter = null;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;

    //private ExpandableListView mGattServicesList;
    private BluetoothService mBluetoothService = null;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    private boolean btConnected = false;

    public final static UUID HM_RX_TX = UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Widgets
    // Status TextViews
    @BindView(R.id.tvBatt)
    TextView batteryTV;
    @BindView(R.id.tvDevice)
    TextView timeOnTV;
    @BindView(R.id.tvEnv)
    TextView tempEnvTV;
    @BindView(R.id.tvWat)
    TextView tempWatterTV;

    // Main "Buttons" - Actions
    @BindView(R.id.llMain)
    LinearLayout mainLayout;
    @BindView(R.id.llOn)
    LinearLayout powerLayout;
    @BindView(R.id.llLight)
    LinearLayout lightLayout;
    @BindView(R.id.llTrack)
    LinearLayout trackLayout;

    // Layout views (Navigation, Drawer and toolbar)
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Widgets binding and setting
        ButterKnife.bind(this);
        mainLayout.setPadding(16, 16, 16, 16);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(getBaseContext().BLUETOOTH_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

            if (btConnected) {
                if (mBluetoothService != null) {
                    final boolean result = mBluetoothService.connect(mDeviceAddress);
                    Log.d("Main-Bluetooth", "Connect request result = " + result);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothService = null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Menu and drawer region

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_connect)
        {
            startActivityForResult(new Intent(getBaseContext(), DeviceListActivity.class),
                    REQUEST_CONNECT_DEVICE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_bt) {
            // Launch the DeviceListActivity to see devices and do scan
            startActivityForResult(new Intent(getBaseContext(), DeviceListActivity.class),
                    REQUEST_CONNECT_DEVICE);

            return true;
        }
        else if (id == R.id.nav_set) {
            // Launch the SettingsActivity to change the preferences
            startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class),
                    REQUEST_GENERAL_SETTINGS);

            return true;
        }
        else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);

            sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_texts));
            sendIntent.setType("text/plain");

            // Verify that the intent will resolve to an activity
            if (sendIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(sendIntent, getResources().
                        getString(R.string.send_share)));
            }
            else {
                Toast.makeText(getBaseContext(), getResources().
                                getString(R.string.error_share),
                        Toast.LENGTH_SHORT).show();
            }

            return true;
        }
        else if (id == R.id.nav_send) {
            Intent mail_intent = new Intent(Intent.ACTION_SENDTO);

            mail_intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            mail_intent.putExtra(Intent.EXTRA_EMAIL, getResources().getStringArray(R.array.email));
            mail_intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.subject_mail));

            // Verify that the intent will resolve to an activity
            if (mail_intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(mail_intent, getResources()
                        .getString(R.string.send_mail)));
            }
            else {
                Toast.makeText(getBaseContext(), getResources()
                                .getString(R.string.error_mail),
                        Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }


    // end Region

    // Dialog Region (Used at "On/Off" button)

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
        Toast.makeText(getBaseContext(), "Turned Off", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
        Toast.makeText(getBaseContext(), "Canceled", Toast.LENGTH_SHORT).show();
    }

    // end Region

    //region click listeners

    @OnClick(R.id.llOn)
    public void powerClicked() {
        if (btConnected)
        {
            // return the code here
        }
        DialogFragment newFragment = new QuatroDialogFragment();
        newFragment.show(MainActivity.this.getFragmentManager(), "Confirm");
    }

    @OnClick(R.id.llLight)
    public void lightClicked()
    {
        if (btConnected)
        {
            // return the code here
        }
        startActivityForResult(new Intent(getBaseContext(), LightSettingsActivity.class),
                REQUEST_LIGHT_SETTINGS);
    }

    @OnClick(R.id.llTrack)
    public void trackClicked()
    {
        if (btConnected)
        {
            //Intent mapsIntent = new Intent(getBaseContext(), MapsActivity.class);
            //startActivityForResult(mapsIntent, REQUEST_TRACKING_SCREEN);
        }
        Toast.makeText(getBaseContext(), "Track!", Toast.LENGTH_SHORT).show();
    }

    //end region


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GENERAL_SETTINGS:
                // When Settings returns with a Language change
                if (resultCode == Activity.RESULT_OK) {
                    recreate();
                }
                else if (resultCode == Activity.RESULT_FIRST_USER) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                break;

            case REQUEST_ENABLE_BT:
                if (resultCode != Activity.RESULT_OK) {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    mDeviceAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    connectDevice();
                }
                break;
        }
    }


    // Bluetooth region

    private void connectDevice ()
    {
        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
            if (!mBluetoothService.initialize()) {
                Log.e("Main-Bluetooth", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                btConnected = true;
                updateConnectionState();
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                btConnected = false;
                updateConnectionState();
//                clearUI();
            }
//            else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothService.getSupportedGattServices());
//            }
            else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(mBluetoothService.EXTRA_DATA));
            }
        }
    };

//    private void clearUI() {
//        mDataField.setText(R.string.no_data);
//    }


    private void updateConnectionState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mConnectionState.setText(resourceId);
                // Change the enable properties of the widgets
                if (btConnected) {

                }
                else {

                }
            }
        });
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
//    private void displayGattServices(List<BluetoothGattService> gattServices) {
//        if (gattServices == null) return;
//        String uuid = null;
//        String unknownServiceString = getResources().getString(R.string.unknown_service);
//        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
//
//
//        // Loops through available GATT Services.
//        for (BluetoothGattService gattService : gattServices) {
//            HashMap<String, String> currentServiceData = new HashMap<String, String>();
//            uuid = gattService.getUuid().toString();
//            currentServiceData.put(
//                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
//
//            // If the service exists for HM 10 Serial, say so.
//            if(SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") { isSerial.setText("Yes, serial :-)"); } else {  isSerial.setText("No, serial :-("); }
//            currentServiceData.put(LIST_UUID, uuid);
//            gattServiceData.add(currentServiceData);
//
//            // get characteristic when UUID matches RX/TX UUID
//            characteristicTX = gattService.getCharacteristic(BluetoothService.UUID_HM_RX_TX);
//            characteristicRX = gattService.getCharacteristic(BluetoothService.UUID_HM_RX_TX);
//        }
//
//    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        //intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    // on change of bars write char
    private void sendChar() {
        String msg = "<OCTO>\n";
        Log.d("Main-Bluetooth", "Sending message = " + msg);
        final byte[] tx = msg.getBytes();
        if(btConnected) {
            characteristicTX.setValue(tx);
            mBluetoothService.writeCharacteristic(characteristicTX);
            mBluetoothService.setCharacteristicNotification(characteristicRX,true);
        }
    }


}
