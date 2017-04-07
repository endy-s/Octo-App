package com.br.octo.board;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, QuatroDialogFragment.QuatroDialogListener {

    private static final String TAG = "MainBluetooth";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE      = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE    = 2;
    private static final int REQUEST_GENERAL_SETTINGS           = 3;
    private static final int REQUEST_LIGHT_SETTINGS             = 4;
    private static final int REQUEST_TRACKING_SCREEN            = 4;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mBluetoothService = null;

    /**
     * "Share" variable
     */
//    private ShareActionProvider mShareActionProvider;

    /**
     * Widgets variables
     */
    private ImageView    ivPower, ivLight, ivTrack;
    private TextView     tvPower, tvLight, tvTrack, tvBat, tvDev, tvEnv, tvWater;
    private LinearLayout llPower, llLight, llTrack;

    /**
     * Flags variables
     */
    private boolean btConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fillWidget();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    // Initialize the BluetoothChatService to perform bluetooth connections
        mBluetoothService = new BluetoothService(this, mHandler);
    }

    private void fillWidget() {

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.llMain);
        mainLayout.setPadding(16, 16, 16, 16);
        //ivPower = (ImageView) findViewById(R.id.ivPower);
        //ivLight = (ImageView) findViewById(R.id.ivLight);
        //ivTrack = (ImageView) findViewById(R.id.ivTrack);

        tvBat   = (TextView)    findViewById(R.id.tvBatt);
        tvDev   = (TextView)    findViewById(R.id.tvDevice);
        tvEnv   = (TextView)    findViewById(R.id.tvEnv);
        tvWater = (TextView)    findViewById(R.id.tvWat);

        llPower = (LinearLayout) findViewById(R.id.llOn);
        llLight = (LinearLayout) findViewById(R.id.llLight);
        llTrack = (LinearLayout) findViewById(R.id.llTrack);

        llPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btConnected)
                {
                    Toast.makeText(getBaseContext(), "Power!", Toast.LENGTH_SHORT).show();
                }
                DialogFragment newFragment = new QuatroDialogFragment();
                newFragment.show(MainActivity.this.getFragmentManager(), "Confirm");
            }
        });

        llLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btConnected)
                {
                    Toast.makeText(getBaseContext(), "Light!", Toast.LENGTH_SHORT).show();
                }
                Intent lightSettingsIntent = new Intent(getBaseContext(), LightSettingsActivity.class);
                startActivityForResult(lightSettingsIntent, REQUEST_LIGHT_SETTINGS);
            }
        });

        llTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btConnected)
                {
                    Intent mapsIntent = new Intent(getBaseContext(), MapsActivity.class);
                    startActivityForResult(mapsIntent, REQUEST_TRACKING_SCREEN);
                }
                Toast.makeText(getBaseContext(), "Track!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_connect)
        {
            Intent serverIntent = new Intent(getBaseContext(), DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        }
//        else if (R.id.discoverable)
//          {
//                // Ensure this device is discoverable by others
//                ensureDiscoverable();
//                return true;
//            }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_bt)
        {
        // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(getBaseContext(), DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        }
        else if (id == R.id.nav_set)
        {
            // Launch the SettingsActivity to change the preferences
            Intent settingsIntent = new Intent(getBaseContext(), SettingsActivity.class);
            startActivityForResult(settingsIntent, REQUEST_GENERAL_SETTINGS);
            return true;
        }
        //        else if (id == R.id.nav_history)
        // {
//            Toast.makeText(this, "History!", Toast.LENGTH_LONG).show();
//        }
        else if (id == R.id.nav_share)
        {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);

            sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_texts));
            sendIntent.setType("text/plain");

            // Verify that the intent will resolve to an activity
            if (sendIntent.resolveActivity(getPackageManager()) != null)
            {
                startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.send_share)));
            }
            else
            {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.error_share), Toast.LENGTH_SHORT).show();
            }

        }
        else if (id == R.id.nav_send)
        {
            Intent mail_intent = new Intent(Intent.ACTION_SENDTO);

            mail_intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            mail_intent.putExtra(Intent.EXTRA_EMAIL, getResources().getStringArray(R.array.email));
            mail_intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.subject_mail));
            if (mail_intent.resolveActivity(getPackageManager()) != null)
            {
                startActivity(Intent.createChooser(mail_intent, getResources().getString(R.string.send_mail)));
            }
            else
            {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.error_mail), Toast.LENGTH_SHORT).show();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            MainActivity.this.getSupportActionBar().setSubtitle(R.string.title_connected);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            MainActivity.this.getSupportActionBar().setSubtitle(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            MainActivity.this.getSupportActionBar().setSubtitle(R.string.title_not_connected);
                            break;
                    }
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
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetoothService.connect(device, secure);
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != mBluetoothService.STATE_CONNECTED) {
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

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;

            case REQUEST_GENERAL_SETTINGS:
                // When Settings returns with a Language change
                recreate();
//                if (resultCode == Activity.RESULT_OK) {
//                    connectDevice(data, false);
//                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
}
