package com.br.octo.board.modules.main;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.models.Paddle;
import com.br.octo.board.modules.DeviceListActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.settings.LightSettingsActivity;
import com.br.octo.board.modules.settings.LocaleHelper;
import com.br.octo.board.modules.settings.SettingsActivity;
import com.br.octo.board.modules.tracking.PaddleActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import zh.wang.android.yweathergetter4a.WeatherInfo;
import zh.wang.android.yweathergetter4a.YahooWeather;
import zh.wang.android.yweathergetter4a.YahooWeatherInfoListener;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.br.octo.board.Constants.REQUEST_ENABLE_BT_TO_SCAN;
import static com.br.octo.board.Constants.REQUEST_GENERAL_SETTINGS;
import static com.br.octo.board.Constants.REQUEST_LIGHT_SETTINGS;
import static com.br.octo.board.Constants.REQUEST_SCAN_DEVICE;
import static com.br.octo.board.Constants.REQUEST_TRACKING_SCREEN;
import static com.br.octo.board.Constants.actualPaddleId;
import static com.br.octo.board.Constants.battValue;

/**
 * Created by Endy.
 */
public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener,
        BluetoothHelper.BluetoothCallback, YahooWeatherInfoListener {

    // Bluetooth
    BluetoothHelper btHelper;
    private BluetoothAdapter mBluetoothAdapter = null;

    // Weather
    private YahooWeather weather;
    private boolean retrievingWeather = false;

    // Paddle "flags"
    private int paddleId = 0;
    private boolean startedPaddling = false;

    //Light Flag
    private boolean scanFromLights = false;

    // Widgets
    // Status TextViews
    @BindView(R.id.txtBattery)
    TextView batteryTV;
    @BindView(R.id.txtBoard)
    TextView boardTV;
    @BindView(R.id.txtAmbient)
    TextView tempEnvTV;
    @BindView(R.id.txtAmbientProgress)
    ProgressBar tempEnvProgress;
    @BindView(R.id.txtWater)
    TextView tempWaterTV;
    @BindView(R.id.txtWaterProgress)
    ProgressBar tempWaterProgress;

    // Last Paddle Info
    @BindView(R.id.lastDistTV)
    TextView lastDist;
    @BindView(R.id.lastTimeTV)
    TextView lastDuration;
    @BindView(R.id.lastKcalTV)
    TextView lastKcal;
    @BindView(R.id.lastDateTV)
    TextView lastDate;

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
//        setTitle("");
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        showLastPaddleInfo();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btHelper = BluetoothHelper.getInstance();

        weather = YahooWeather.getInstance();
        tempClicked();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (btHelper.getConnectionStatus()) {
            showConnectedState();
        } else {
            showNotConnectedState();
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
            if (startedPaddling) {
                createDialog(R.string.dialog_reconnected_title, R.string.dialog_finish_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        if (item.getItemId() == R.id.action_connect) {
            checkBTConnectionToScan();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_bt: {
                checkBTConnectionToScan();
                break;
            }
            case R.id.nav_set: {
                startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class), REQUEST_GENERAL_SETTINGS);
                break;
            }
            case R.id.nav_light: {
                checkBTConnectionToLight();
                break;
            }
            case R.id.nav_history: {
                // TODO: Call the History view (to be developed)
                Toast.makeText(getBaseContext(), "Coming soon", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.nav_tutorial: {
                // TODO: Call the Tutorial view (to be developed)
                Toast.makeText(getBaseContext(), "Coming soon", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.nav_share: {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);

                sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_texts));
                sendIntent.setType("text/plain");

                if (sendIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(sendIntent, getResources().
                            getString(R.string.send_share)));
                } else {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.error_share), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.nav_send: {
                Intent mail_intent = new Intent(Intent.ACTION_SENDTO);

                mail_intent.setData(Uri.parse("mailto:"));
                mail_intent.putExtra(Intent.EXTRA_EMAIL, getResources().getStringArray(R.array.email));
                mail_intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.subject_mail));

                if (mail_intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(mail_intent, getResources()
                            .getString(R.string.send_mail)));
                } else {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.error_mail), Toast.LENGTH_SHORT).show();
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
        startedPaddling = true;
        Intent trackingIntent = new Intent(getBaseContext(), PaddleActivity.class);
        trackingIntent.putExtra(actualPaddleId, paddleId);
        trackingIntent.putExtra(battValue, batteryTV.getText().toString().replace("%", ""));
        startActivityForResult(trackingIntent, REQUEST_TRACKING_SCREEN);
    }

    @OnClick({R.id.txtAmbient, R.id.txtWater})
    public void tempClicked() {
        if (!retrievingWeather) {
            retrievingWeather = true;

            tempEnvTV.setText(getString(R.string.bt_unknown));
            tempEnvTV.setVisibility(INVISIBLE);
            tempEnvProgress.setVisibility(VISIBLE);

            tempWaterTV.setText(getString(R.string.bt_unknown));
            tempWaterTV.setVisibility(INVISIBLE);
            tempWaterProgress.setVisibility(VISIBLE);

            weather.queryYahooWeatherByGPS(this, this);
        }
    }

    //endregion

    //region Results

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GENERAL_SETTINGS:
                if (resultCode == RESULT_OK) {
                    recreate();
                } else if (resultCode == Activity.RESULT_FIRST_USER) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                break;

            case REQUEST_ENABLE_BT_TO_SCAN:
                if (resultCode != RESULT_OK) {
                    createDialog(R.string.error_bt_not_wanted_title, R.string.error_bt_not_wanted_message)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                } else {
                    showBTDeviceScanScreen();
                }
                break;

            case REQUEST_SCAN_DEVICE:
                if (resultCode == RESULT_OK) {
                    showConnectedState();
                    if (scanFromLights) {
                        scanFromLights = false;
                        showLightScreen();
                    }
                }
                break;
            case REQUEST_TRACKING_SCREEN:
                if (resultCode == RESULT_OK) {
                    startedPaddling = false;
                    showLastPaddleInfo();
                }
                break;
        }
    }

    //endregion

    //region Private - manage the info shown at the screen

    private void showLastPaddleInfo() {
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().build();
//        Realm.deleteRealm(realmConfiguration);
        Realm realm = Realm.getInstance(realmConfiguration);
        if (realm.where(Paddle.class).findAllSorted("id").size() > 0) {
            Paddle lastPaddle = realm.where(Paddle.class).findAllSorted("id").last();

            if (lastPaddle != null) {
                paddleId = lastPaddle.getId() + 1;

                lastDist.setText(String.format("%.2f %s", lastPaddle.getDistance(), getString(R.string.bt_dist)));
                lastKcal.setText(String.format("%d %s", lastPaddle.getKcal(), getString(R.string.bt_kcal)));

                int hour = (int) lastPaddle.getDuration() / (60 * 60);
                int minutes = (int) (lastPaddle.getDuration() / 60) % 60;
                lastDuration.setText(String.format("%02d:%02d %s", hour, minutes, getString(R.string.bt_hour)));

                SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
                lastDate.setText(dateFormatter.format(lastPaddle.getDate()));
            }
        }
        realm.close();
    }

    private void showNotConnectedState() {
        batteryTV.setText(R.string.bt_unknown);
        boardTV.setText(R.string.bt_board_off);
    }

    private void showConnectedState() {
        boardTV.setText(R.string.bt_board_on);
    }

    private void checkBTConnectionToScan() {
        if (btHelper.getConnectionStatus()) {
            createDialog(R.string.dialog_reconnected_title, R.string.dialog_reconnected_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showBTDeviceScanScreen();
                            btHelper.disconnect();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT_TO_SCAN);
            } else {
                showBTDeviceScanScreen();
            }
        }
    }

    private void checkBTConnectionToLight() {
        if (!btHelper.getConnectionStatus()) {
            createDialog(R.string.dialog_not_connected_light_title, R.string.dialog_not_connected_light_message)
                    .setPositiveButton(R.string.dialog_not_connected_light_positive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            scanFromLights = true;
                            checkBTConnectionToScan();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            showLightScreen();
        }
    }

    private void showBTDeviceScanScreen() {
        startActivityForResult(new Intent(getBaseContext(), DeviceListActivity.class), REQUEST_SCAN_DEVICE);
    }

    private void showLightScreen() {
        startActivityForResult(new Intent(getBaseContext(), LightSettingsActivity.class), REQUEST_LIGHT_SETTINGS);
    }

    //endregion

    //region BT Callback

    @Override
    public void onMessageReceived(String message) {
        Log.d("Main", "BT Received: " + message);
        if (message.startsWith("B")) {
            final String battValue = message.split(";")[0];
//            final String tempValue = message.split(";")[1];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    batteryTV.setText(battValue.substring(2).trim().concat("%"));
//                    tempWaterTV.setText(tempValue.substring(2).trim().concat(" °C"));
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

    //region Yahoo Weather Callback

    @Override
    public void gotWeatherInfo(WeatherInfo weatherInfo, YahooWeather.ErrorType errorType) {
        retrievingWeather = false;

        tempEnvTV.setVisibility(VISIBLE);
        tempEnvProgress.setVisibility(INVISIBLE);

        tempWaterTV.setVisibility(VISIBLE);
        tempWaterProgress.setVisibility(INVISIBLE);
        
        if (errorType != null) {
            tempEnvTV.setText(R.string.bt_temp_NA);
            tempWaterTV.setText(R.string.bt_temp_NA);
        }
        if (weatherInfo != null) {
            tempEnvTV.setText(String.valueOf(weatherInfo.getCurrentTemp()).concat(" °C"));
            tempWaterTV.setText(String.valueOf(weatherInfo.getCurrentTemp() - 5).concat(" °C"));
        }
    }

    //endregion


}
