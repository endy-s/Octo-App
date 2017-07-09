package com.br.octo.board.modules.tracking;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.models.Paddle;
import com.br.octo.board.models.TrackingPoints;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.end.EndPaddleActivity;
import com.br.octo.board.modules.settings.LightSettingsActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.quentinklein.slt.LocationTracker;
import fr.quentinklein.slt.TrackerSettings;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.br.octo.board.Constants.REQUEST_LIGHT_SETTINGS;
import static com.br.octo.board.Constants.actualPaddleId;
import static com.br.octo.board.Constants.battValue;

public class PaddleActivity extends BaseActivity implements
        BottomNavigationView.OnNavigationItemSelectedListener, BluetoothHelper.BluetoothCallback {

    LocationTracker tracker;

    static ArrayList<TrackingPoints> route = new ArrayList<>();

    float kmPaddling = 0;
    float actualSpeed = 0;
    long timeWhenStopped = 0;
    private boolean trackingRunning = true;
    private int paddleId = 0;

    BluetoothHelper btHelperPaddle;

    ProgressDialog endingProgressDialogs;

    GoogleMap gMap;

    //Widgets
    @BindView(R.id.btLight)
    ImageButton btLight;
    @BindView(R.id.txtBatteryPaddle)
    TextView txtBatPaddle;
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

        txtTime.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            public void onChronometerTick(Chronometer cArg) {
                long actualTime = (SystemClock.elapsedRealtime() - cArg.getBase()) / 1000;

                int hour = (int) actualTime / (60 * 60);
                int minutes = (int) (actualTime / 60) % 60;
                cArg.setText(String.format("%02d:%02d", hour, minutes));
            }
        });

        btHelperPaddle = BluetoothHelper.getInstance();

        if (getIntent().hasExtra(actualPaddleId)) {
            paddleId = getIntent().getIntExtra(actualPaddleId, 0);

            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().build();
            Realm realm = Realm.getInstance(realmConfiguration);

            if (realm.where(Paddle.class).equalTo("id", paddleId).findFirst() != null) {
                Paddle resumingPaddle = realm.copyFromRealm(realm.where(Paddle.class).equalTo("id", paddleId).findFirst());
                setResumingPaddleInfo(resumingPaddle);
            }

            realm.close();
        }


        if (getIntent().hasExtra(battValue)) {
            txtBatPaddle.setText(getIntent().getStringExtra(battValue));
        }

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
                    actualSpeed = (location.getSpeed() * 3.6f);

                    if (route.size() > 1) {
                        float[] results = new float[3];
                        Location.distanceBetween(
                                route.get(route.size() - 1).getLatitude(),
                                route.get(route.size() - 1).getLongitude(),
                                location.getLatitude(),
                                location.getLongitude(),
                                results);

                        kmPaddling += results[0] / 1000;
                    } else {
                        kmPaddling = 0;
                    }

                    route.add(new TrackingPoints(location.getLatitude(), location.getLongitude()));

                    txtKm.setText(String.format("%.2f", kmPaddling));
                    txtSpeed.setText(String.format("%.2f", actualSpeed));
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
        btHelperPaddle.setCallback(this);
    }

    @Override
    protected void onDestroy() {
        txtKm.setText(R.string.bt_unknown);
        txtRows.setText(R.string.bt_unknown);
        txtKcal.setText(R.string.bt_unknown);
        txtSpeed.setText(R.string.bt_unknown);
        txtBatPaddle.setText(R.string.bt_unknown);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        storePaddleInfo();
        finish();
    }

    //endregion

    //region widget listeners

    @OnClick(R.id.btLight)
    public void LightClicked() {
        Intent lightIntent = new Intent(getBaseContext(), LightSettingsActivity.class);
        startActivityForResult(lightIntent, REQUEST_LIGHT_SETTINGS);
    }

    @OnClick(R.id.btMaps)
    public void showMap() {
        if (route.size() > 0) {
            showMapDialog();
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

                stopTracking(true);

                Paddle actualPaddle = storePaddleInfo();
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

    private void setResumingPaddleInfo(Paddle resumingPaddle) {
        kmPaddling = resumingPaddle.getDistance();
        txtKm.setText(String.format("%.2f", kmPaddling));

        actualSpeed = 0;
        txtSpeed.setText(String.format("%.2f", actualSpeed));

        timeWhenStopped = resumingPaddle.getDuration() * (-1000);

        RealmList<TrackingPoints> resumePaddlePoints = resumingPaddle.getTrack();

        for (int index = 0; index < resumePaddlePoints.size(); index++) {
            route.add(resumePaddlePoints.get(index));
        }
    }

    public Paddle storePaddleInfo() {
        RealmList<TrackingPoints> paddlePoints = new RealmList<>();

        for (int index = 0; index < route.size(); index++) {
            paddlePoints.add(route.get(index));
        }

        long duration = (SystemClock.elapsedRealtime() - txtTime.getBase()) / 1000;

//                Paddle actualPaddle = new Paddle(paddleId, "10", "20", "07", "17.06.2017", "10.8", "200", paddlePoints);
        Paddle actualPaddleInfo = new Paddle();
        actualPaddleInfo.setId(paddleId);
        actualPaddleInfo.setDate(Calendar.getInstance().getTime().getTime());
        actualPaddleInfo.setDistance(kmPaddling);
        actualPaddleInfo.setDuration(duration);
        actualPaddleInfo.setRows(1);
        actualPaddleInfo.setKcal(2);
        actualPaddleInfo.setSpeed(((kmPaddling * 1000) / duration) * 3.6f);
        actualPaddleInfo.setTrack(paddlePoints);

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().build();
        Realm realm = Realm.getInstance(realmConfiguration);

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(actualPaddleInfo);
        realm.commitTransaction();

        realm.close();

        return actualPaddleInfo;
    }

    private void showMapDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(PaddleActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_map, null);
        final MapView dialogMapView = (MapView) mView.findViewById(R.id.dialogMap);

        mBuilder.setView(mView);

        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        dialogMapView.onCreate(new Bundle());

        try {
            MapsInitializer.initialize(this.getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        dialogMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                gMap = mMap;

                int numberPoints = route.size();

                if (numberPoints > 0) {
                    LatLng stop = new LatLng(route.get(numberPoints - 1).getLatitude(), route.get(numberPoints - 1).getLongitude());
                    gMap.addMarker(new MarkerOptions().position(stop).title("End").snippet("End of Paddling")).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));

                    PolylineOptions lineOptions = new PolylineOptions().width(5).color(Color.RED);
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                    for (int index = 0; index < numberPoints; index++) {
                        LatLng point = new LatLng(route.get(index).getLatitude(), route.get(index).getLongitude());
                        lineOptions.add(point);
                        builder.include(point);
                    }

                    Polyline line = gMap.addPolyline(lineOptions);
                    LatLngBounds bounds = builder.build();

                    int padding = 25;
                    CameraUpdate zoom = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                    gMap.moveCamera(zoom);
                    dialogMapView.onResume();
                }
            }
        });

        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    //endregion

    //region BT Callback

    @Override
    public void onMessageReceived(String message) {
        Log.d("Paddle", "BT Received: " + message);
        if (message.startsWith("B")) {
            final String battValue = message.split(";")[0];
//            final String tempValue = message.split(";")[1];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtBatPaddle.setText(battValue.substring(2).trim());
//                    tempWatterTV.setText(tempValue.substring(2).trim().concat(" °C"));
                }
            });
        }
    }

    @Override
    public void onDeviceConnected() {
    }

    @Override
    public void onDeviceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createDialog(R.string.bt_disconnect_title, R.string.bt_disconnect_message)
                        .setPositiveButton(R.string.ok, null).show();
            }
        });
    }

    //endregion
}
