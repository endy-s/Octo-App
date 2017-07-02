package com.br.octo.board.modules.tracking;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.br.octo.board.Constants;
import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.models.Paddle;
import com.br.octo.board.models.TrackingPoints;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.end.EndPaddleActivity;
import com.br.octo.board.modules.settings.LightSettingsActivity;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.quentinklein.slt.LocationTracker;
import fr.quentinklein.slt.ProviderError;
import fr.quentinklein.slt.TrackerSettings;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class PaddleActivity extends BaseActivity implements
        BottomNavigationView.OnNavigationItemSelectedListener, BluetoothHelper.BluetoothCallback {

    LocationTracker tracker;

    static ArrayList<TrackingPoints> route = new ArrayList<>();

    float kmPaddling = 0;
    float averageSpeed = 0;
    long timeWhenStopped = 0;
    private boolean trackingRunning = true;

    ProgressDialog endingProgressDialogs;

    //Widgets
    @BindView(R.id.btLight)
    ImageButton btLight;
    @BindView(R.id.txtBatteryPaddle)
    TextView txtBat;
    @BindView(R.id.btMaps)
    ImageButton btMaps;
    @BindView(R.id.bottomController)
    BottomNavigationView bottomController;

    @BindView(R.id.txtKm)
    TextView txtKm;
    @BindView(R.id.txtRows)
    TextView txtRows;
    @BindView(R.id.txtTime)
    Chronometer txtTime;
    @BindView(R.id.txtKcal)
    TextView txtKcal;
    @BindView(R.id.txtSpeed)
    TextView txtSpeed;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_tracking);
        ButterKnife.bind(this);

        bottomController.setOnNavigationItemSelectedListener(this);

        if (ContextCompat.checkSelfPermission(getBaseContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getBaseContext(), ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            Log.d("PERMISSION", "NOT GRANTED");
        } else {

            TrackerSettings settings = new TrackerSettings()
                    .setUseGPS(true)
                    .setUseNetwork(false)
                    .setUsePassive(false)
                    .setTimeBetweenUpdates(30)
                    .setMetersBetweenUpdates(1f);

            tracker = new LocationTracker(getBaseContext(), settings) {
                @Override
                public void onLocationFound(Location location) {
                    averageSpeed += (location.getSpeed() * 3.6f);

                    if (route.size() > 1) {
                        averageSpeed = averageSpeed / 2;

                        float[] results = new float[3];
                        Location.distanceBetween(
                                route.get(route.size() - 1).getLatitude(),
                                route.get(route.size() - 1).getLongitude(),
                                location.getLatitude(),
                                location.getLongitude(),
                                results);

                        kmPaddling += results[0];

                        Log.d("Distance and Speed", String.format("Distance: %.2f and Speed: %.2f", results[0], location.getSpeed()));
                    } else {
                        kmPaddling = 0;
                    }

                    route.add(new TrackingPoints(location.getLatitude(), location.getLongitude()));

                    txtKm.setText(String.format("%.2f", kmPaddling));
                    txtSpeed.setText(String.format("%.2f", averageSpeed));
                }

                @Override
                public void onTimeout() {
                    Log.d("Location Timeout", "Timeout! Restarting...");
                    tracker.startListening();
                }
            };
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startTracking();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTracking(false);
    }

    @Override
    protected void onDestroy() {
        txtKm.setText(R.string.bt_unknown);
        txtRows.setText(R.string.bt_unknown);
        txtKcal.setText(R.string.bt_unknown);
        txtSpeed.setText(R.string.bt_unknown);
        txtBat.setText(R.string.bt_unknown);
        route.clear();
        if ((endingProgressDialogs != null) && (endingProgressDialogs.isShowing()))
            endingProgressDialogs.dismiss();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region widget listeners

    @OnClick(R.id.btLight)
    public void LightClicked() {
        Intent lightIntent = new Intent(getBaseContext(), LightSettingsActivity.class);
        startActivityForResult(lightIntent, Constants.REQUEST_LIGHT_SETTINGS);
    }

    @OnClick(R.id.btMaps)
    public void showMap() {
        if (route.size() > 0) {

        } else {
            createDialog(R.string.no_location_title, R.string.no_location_message)
                    .setPositiveButton(R.string.ok, null).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_pause: {
                trackingRunning = !trackingRunning;

                if (!trackingRunning) {
                    item.setTitle(R.string.bt_play);
                    item.setIcon(R.drawable.ic_play);
                    stopTracking(false);
                } else {
                    item.setTitle(R.string.bt_pause);
                    item.setIcon(R.drawable.ic_pause);
                    startTracking();
                }
                break;
            }
            case R.id.item_stop: {
                endingProgressDialogs = ProgressDialog.show(this, getResources().getString(R.string.end_progress_title), getResources().getString(R.string.end_progress_message), true, false);

                RealmList<TrackingPoints> paddlePoints = new RealmList<>();
                paddlePoints.clear();

                for (int index = 0; index < route.size(); index++) {
                    paddlePoints.add(route.get(index));
                }

//                Paddle actualPaddle = new Paddle("10", "20", "07", "17.06.2017", "10.8", "200", paddlePoints);
                Paddle actualPaddle = new Paddle();
                actualPaddle.setDate(Calendar.getInstance().getTime().getTime());
                actualPaddle.setDistance(kmPaddling);
                actualPaddle.setDuration((SystemClock.elapsedRealtime() - txtTime.getBase()) / 1000);
                actualPaddle.setRows(1);
                actualPaddle.setKcal(2);
                actualPaddle.setSpeed(averageSpeed);
                actualPaddle.setTrack(paddlePoints);

                stopTracking(true);

                storePaddleInfo(actualPaddle);

                Intent endPaddleIntent = new Intent(getBaseContext(), EndPaddleActivity.class);

                endPaddleIntent.putExtra(getString(R.string.paddle_extra), Parcels.wrap(actualPaddle));
                startActivity(endPaddleIntent);
            }
        }
        return false;
    }

    //end region

    //region Private

    private void startTracking() {
        if (!tracker.isListening()) {
            tracker.startListening();
            txtTime.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
            txtTime.start();
        }
    }

    private void stopTracking(Boolean stop) {
        if (tracker.isListening()) {
            tracker.stopListening();

            if (stop) {
                timeWhenStopped = 0;
            } else {
                timeWhenStopped = txtTime.getBase() - SystemClock.elapsedRealtime();
            }

            txtTime.stop();
        }
    }

    public void storePaddleInfo(Paddle actualPaddleInfo) {
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().build();
        Realm realm = Realm.getInstance(realmConfiguration);
        realm.beginTransaction();
        realm.copyToRealm(actualPaddleInfo);
        realm.commitTransaction();
        realm.close();
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
                    txtBat.setText(battValue.substring(2).trim());
//                    tempWatterTV.setText(tempValue.substring(2).trim().concat(" °C"));
                }
            });
        }
    }

    @Override
    public void onDeviceConnected() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                showConnectedState();
//            }
//        });
    }

    @Override
    public void onDeviceDisconnected() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                showNotConnectedState();
//            }
//        });
    }

    //endregion
}
