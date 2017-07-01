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
import com.br.octo.board.models.Paddle;
import com.br.octo.board.models.TrackingPoints;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.end.EndPaddleActivity;
import com.br.octo.board.modules.settings.LightSettingsActivity;
import com.google.android.gms.maps.model.LatLng;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

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
import static com.br.octo.board.Constants.REQUEST_END_SCREEN;

public class PaddleActivity extends BaseActivity implements
        BottomNavigationView.OnNavigationItemSelectedListener {

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
    @BindView(R.id.btShare)
    ImageButton btShare;
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
                    //TODO remove this, used only for testing @ dev
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d("Location Update", "New Location: " + latLng.toString());

                    route.add(new TrackingPoints(location.getLatitude(), location.getLongitude()));

                    float[] results = new float[3];
                    Location.distanceBetween(
                            route.get(route.size() - 1).getLatitude(),
                            route.get(route.size() - 1).getLongitude(),
                            location.getLatitude(),
                            location.getLongitude(),
                            results);

                    kmPaddling += results[0];
                    txtKm.setText(String.format(Locale.US, "%.2f", kmPaddling));

                    averageSpeed += (location.getSpeed() * 3.6f);
                    if (route.size() > 0) averageSpeed = averageSpeed / 2;
                    txtSpeed.setText(String.format(Locale.US, "%.2f", averageSpeed));

                    Log.d("Distance and Speed", String.format("Distance: %.2f and Speed: %.2f", results[0], location.getSpeed()));
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_END_SCREEN:
                endingProgressDialogs.dismiss();
                break;
        }
    }

    //endregion

    //region widget listeners

    @OnClick(R.id.btLight)
    public void LightClicked() {
        Intent lightIntent = new Intent(getBaseContext(), LightSettingsActivity.class);
        startActivityForResult(lightIntent, Constants.REQUEST_LIGHT_SETTINGS);
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
                startActivityForResult(endPaddleIntent, REQUEST_END_SCREEN);
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
                txtKm.setText(getString(R.string.bt_unknown));
                txtRows.setText(getString(R.string.bt_unknown));
                txtKcal.setText(getString(R.string.bt_unknown));
                txtSpeed.setText(getString(R.string.bt_unknown));
                timeWhenStopped = 0;
                route.clear();
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
}
