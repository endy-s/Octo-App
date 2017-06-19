package com.br.octo.board.modules.tracking;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import com.br.octo.board.Constants;
import com.br.octo.board.R;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.end.EndPaddleActivity;
import com.br.octo.board.modules.settings.LightSettingsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.quentinklein.slt.LocationTracker;
import fr.quentinklein.slt.TrackerSettings;

public class PaddleActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        BottomNavigationView.OnNavigationItemSelectedListener {

    LocationTracker tracker;
    GoogleMap mMap;

    static ArrayList<LatLng> route = new ArrayList();

    public static GoogleApiClient mGoogleApiClient;
    public LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 30000;  /* 30 secs */
    private long FASTEST_INTERVAL = 5000; /* 5 secs */

    long timeWhenStopped = 0;
    private boolean trackingRunning = true;

    //Widgets
    @BindView(R.id.btLight)
    ImageButton btLight;
    @BindView(R.id.btShare)
    ImageButton btShare;
    @BindView(R.id.btMaps)
    ImageButton btMaps;
    @BindView(R.id.bottomController)
    BottomNavigationView bottomController;

    @BindView(R.id.txtTime)
    Chronometer txtTime;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_tracking);
        ButterKnife.bind(this);

        bottomController.setOnNavigationItemSelectedListener(this);

        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSION", "NOT GRANTED");
        } else {

            TrackerSettings settings = new TrackerSettings()
                    .setUseGPS(true)
                    .setUseNetwork(false)
                    .setUsePassive(false)
                    .setTimeBetweenUpdates(30)
                    .setMetersBetweenUpdates(0.1f);

            tracker = new LocationTracker(getBaseContext(), settings) {

                @Override
                public void onLocationFound(Location location) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d("Location Update", "New Location: " + latLng.toString());

                    route.add(latLng);
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
        pauseTracking();
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_pause: {
                if (trackingRunning) {
                    item.setTitle(R.string.bt_play);
                    item.setIcon(R.drawable.ic_play);
                    trackingRunning = false;
                    pauseTracking();
                } else {
                    item.setTitle(R.string.bt_pause);
                    item.setIcon(R.drawable.ic_pause);
                    trackingRunning = true;
                    startTracking();
                }
                break;
            }
            case R.id.item_stop: {
                startActivity(new Intent(getBaseContext(), EndPaddleActivity.class));
                //TODO Stop everythign
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
            trackingRunning = true;
        }
    }

    private void pauseTracking() {
        if (tracker.isListening()) {
            tracker.stopListening();
            timeWhenStopped = txtTime.getBase() - SystemClock.elapsedRealtime();
            txtTime.stop();
            trackingRunning = false;
        }
    }

    private void stopTracking() {
        //TODO add everything to the RealmClass
        if (tracker.isListening()) {
            tracker.stopListening();
            timeWhenStopped = 0;
            txtTime.stop();
            trackingRunning = false;
        }
    }

    //endregion


    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        UiSettings maps_settings = mMap.getUiSettings();
        maps_settings.setMapToolbarEnabled(true);
        maps_settings.setAllGesturesEnabled(true);
        maps_settings.setCompassEnabled(true);
        maps_settings.setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.MY_LOCATION_REQUEST_CODE);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Constants.MY_LOCATION_REQUEST_CODE) {
            if ((permissions.length == 1) && (permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION) &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(getBaseContext(), "Error with Permission!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /*
     * Called by Location Services when the request to connect the client
     * finishes successfully. At this point, you can request the current
     * location or start periodic updates
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Display the connection status
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(cameraUpdate);
        } else {
            Toast.makeText(getBaseContext(), "Current location was null, enable GPS on settings!", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Called by Location Services if the connection to the location client
     * drops because of an error.
     */
    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(getBaseContext(), "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(getBaseContext(), "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Called by Location Services if the attempt to Location Services fails.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
  /*
             * Google Play services can resolve some errors it detects. If the error
             * has a resolution, try sending an Intent to start a Google Play
             * services activity that can resolve error.
             */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();    // Log the error
            }
        } else {
            Toast.makeText(getBaseContext(), "Sorry. Location services not available to you",
                    Toast.LENGTH_LONG).show();
        }
    }
}
