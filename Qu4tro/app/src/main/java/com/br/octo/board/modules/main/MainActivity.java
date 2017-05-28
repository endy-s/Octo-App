package com.br.octo.board.modules.main;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothService;
import com.br.octo.board.api_services.Constants;
import com.br.octo.board.models.QuatroDialogFragment;
import com.br.octo.board.modules.DeviceListActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.settings.LightSettingsActivity;
import com.br.octo.board.modules.settings.LocaleHelper;
import com.br.octo.board.modules.settings.SettingsActivity;

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
    private static final int REQUEST_CONNECT_DEVICE_INSECURE    = 0;
    private static final int REQUEST_CONNECT_DEVICE_SECURE      = 1;
    private static final int REQUEST_GENERAL_SETTINGS           = 2;
    private static final int REQUEST_LIGHT_SETTINGS             = 3;
    private static final int REQUEST_TRACKING_SCREEN            = 4;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    // Name of the connected device
    private String mConnectedDeviceName = null;


    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothService mBluetoothService = null;

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

    /**
     * Flags variables
     */
    private boolean btConnected = false;

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

    // Initialize the BluetoothChatService to perform bluetooth connections
        mBluetoothService = new BluetoothService(this, mHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
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
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // Menu region (Right BT icon)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_connect)
        {
            startActivityForResult(new Intent(getBaseContext(), DeviceListActivity.class),
                    REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_bt) {
            // Launch the DeviceListActivity to see devices and do scan
            startActivityForResult(new Intent(getBaseContext(), DeviceListActivity.class),
                    REQUEST_CONNECT_DEVICE_SECURE);

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


            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    connectDevice(address, true);
                }
                break;
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param address   An {@link String} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(String address, boolean secure) {

        // Attempt to connect to the device
        mBluetoothService.connect(mBluetoothAdapter.getRemoteDevice(address),
                secure);
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    int new_res_subtitle = R.string.title_not_connected;
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            new_res_subtitle = R.string.title_connected;
                            MainActivity.this.sendMessage("Octo");
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            new_res_subtitle = R.string.title_connecting;
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            new_res_subtitle = R.string.title_not_connected;
                            break;
                    }
                    MainActivity.this.getSupportActionBar().setSubtitle(new_res_subtitle);
                    break;

                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getBaseContext()) {
                        Toast.makeText(getBaseContext(), "Connected to " + mConnectedDeviceName,
                                Toast.LENGTH_SHORT).show();
                    }
                    break;

                case Constants.MESSAGE_TOAST:
                    if (null != getBaseContext()) {
                        Toast.makeText(getBaseContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };


    // Sends a BT message
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

    // Receive a BT message
    private void receiveMessage(String message) {
        // Do the changes at the layouts!
    }
}
