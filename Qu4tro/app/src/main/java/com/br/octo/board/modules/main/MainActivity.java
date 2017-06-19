package com.br.octo.board.modules.main;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.br.octo.board.Constants;
import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.modules.DeviceListActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.settings.LocaleHelper;
import com.br.octo.board.modules.settings.SettingsActivity;
import com.br.octo.board.modules.tracking.PaddleActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Endy.
 */
public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, BluetoothHelper.BluetoothCallback {

// Bluetooth

    BluetoothHelper btHelper;
    private BluetoothAdapter mBluetoothAdapter = null;

    // Widgets
    // Status TextViews
    @BindView(R.id.txtBattery)
    TextView batteryTV;
    @BindView(R.id.txtBoard)
    TextView boardTV;
    @BindView(R.id.txtAmbient)
    TextView tempEnvTV;
    @BindView(R.id.txtWater)
    TextView tempWatterTV;

    // Button
    @BindView(R.id.btStart)
    ImageButton btStart;

    // Layout views (Navigation, Drawer and toolbar)
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;


    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        setTitle("");
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btHelper = BluetoothHelper.getInstance();

        if (!btHelper.getConnectionStatus()) {
            showNotConnectedState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
        }
        btHelper.setCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        if (btHelper.getConnectionStatus()) {
            btHelper.disconnect();
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

    //endregion

    //region Menu and drawer

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_connect) {
            startActivityForResult(new Intent(getBaseContext(), DeviceListActivity.class), Constants.ACTIVITY_REQUEST_SCAN_DEVICE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_bt: {
                startActivityForResult(new Intent(getBaseContext(), DeviceListActivity.class), Constants.ACTIVITY_REQUEST_SCAN_DEVICE);
                break;
            }
            case R.id.nav_set: {
                startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class), Constants.ACTIVITY_REQUEST_GENERAL_SETTINGS);
                break;
            }
            case R.id.nav_history: {
                // TODO: Call the History view (to be developed)
                Toast.makeText(getBaseContext(), "History", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.nav_tutorial: {
                // TODO: Call the Tutorial view (to be developed)
                Toast.makeText(getBaseContext(), "Tutorial", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.nav_share: {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);

                sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_texts));
                sendIntent.setType("text/plain");

                // Verify that the intent will resolve to an activity
                if (sendIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(sendIntent, getResources().
                            getString(R.string.send_share)));
                } else {
                    Toast.makeText(getBaseContext(), getResources().
                                    getString(R.string.error_share),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.nav_send: {
                Intent mail_intent = new Intent(Intent.ACTION_SENDTO);

                mail_intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                mail_intent.putExtra(Intent.EXTRA_EMAIL, getResources().getStringArray(R.array.email));
                mail_intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.subject_mail));

                // Verify that the intent will resolve to an activity
                if (mail_intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(mail_intent, getResources()
                            .getString(R.string.send_mail)));
                } else {
                    Toast.makeText(getBaseContext(), getResources()
                                    .getString(R.string.error_mail),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //endregion

    //region click listeners

    @OnClick(R.id.btStart)
    public void startClicked() {
        if (btHelper.getConnectionStatus()) {
            Intent trackingIntent = new Intent(getBaseContext(), PaddleActivity.class);
            startActivityForResult(trackingIntent, Constants.ACTIVITY_REQUEST_TRACKING_SCREEN);
        }
    }

    //endregion

    //region Results

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.ACTIVITY_REQUEST_GENERAL_SETTINGS:
                // When Settings returns with a Language change
                if (resultCode == RESULT_OK) {
                    recreate();
                } else if (resultCode == Activity.RESULT_FIRST_USER) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                break;

            case Constants.REQUEST_ENABLE_BT:
                if (resultCode != RESULT_OK) {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case Constants.ACTIVITY_REQUEST_SCAN_DEVICE:
                // Check if connected
                if (resultCode == RESULT_OK) {
                    showConnectedState();
                }
                break;
        }
    }

    //endregion

    //region Private - manage the info shown at the screen

    public void showNotConnectedState() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        btStart.setColorFilter(filter);
        btStart.setImageAlpha(128);
        btStart.setEnabled(false);

        tempWatterTV.setText(R.string.bt_unknown);
        tempEnvTV.setText(R.string.bt_unknown);
        batteryTV.setText(R.string.bt_unknown);
        boardTV.setText(R.string.bt_board_off);
    }

    public void showConnectedState() {
        btStart.setColorFilter(null);
        btStart.setImageAlpha(255);
        btStart.setEnabled(true);

        boardTV.setText(R.string.bt_board_on);
    }

    //endregion

    //region BT Callback

    @Override
    public void onMessageReceived(String message) {
        Log.d("Main", "BT Received: " + message);
        if (message.startsWith("B")) {
            final String battValue = message.split(";")[0];
            final String tempValue = message.split(";")[1];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    batteryTV.setText(battValue.substring(2).trim().concat("%"));
                    tempWatterTV.setText(tempValue.substring(2).trim().concat(" °C"));
                }
            });
        }
    }

    @Override
    public void onDeviceConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showConnectedState();
            }
        });
    }

    @Override
    public void onDeviceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showNotConnectedState();
            }
        });
    }

    //endregion


}
