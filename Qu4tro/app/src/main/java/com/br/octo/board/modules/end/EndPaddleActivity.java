package com.br.octo.board.modules.end;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.br.octo.board.R;
import com.br.octo.board.modules.base.BaseActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EndPaddleActivity extends BaseActivity {

    private GoogleMap googleMap;

    //Widgets
    @BindView(R.id.endLayout)
    RelativeLayout endLayout;
    @BindView(R.id.endMap)
    MapView endMapView;
    @BindView(R.id.endShareButton)
    ImageView endShareButton;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_end);
        ButterKnife.bind(this);

        //TODO check if need to show a progressDialog running while the map is not Ready
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.endMap);
//        mapFragment.getMapAsync(this);


        endMapView.onCreate(savedInstanceState);

        try {
            MapsInitializer.initialize(this.getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        endMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                // For dropping a marker at a point on the Map
                Polyline line = googleMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
                        .width(5)
                        .color(Color.RED));
                LatLng london = new LatLng(51.5, -0.1);
                LatLng newYork = new LatLng(40.7, -74.0);

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
//                for (Marker marker : markers) {
//                    builder.include(marker.getPosition());
//                }
                builder.include(london);
                builder.include(newYork);
                LatLngBounds bounds = builder.build();

                googleMap.addMarker(new MarkerOptions().position(newYork).title("Marker Title").snippet("Marker Description"));
                googleMap.addMarker(new MarkerOptions().position(london).title("Marker Title").snippet("Marker Description"));

                int padding = 10;
                CameraUpdate zoom = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                googleMap.moveCamera(zoom);
                googleMap.animateCamera(zoom);
                googleMap.setMyLocationEnabled(true);

                endMapView.onResume(); // needed to get the map to display immediately


                // For zooming automatically to the location of the marker
//                CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(12).build();
//                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        endMapView.onResume(); // needed to get the map to display immediately
    }


    //endregion

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//        mMap = googleMap;
//
//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//    }

    //region click listeners
    @OnClick(R.id.endShareButton)
    public void shareClicked() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US);
        final String fileName = "Paddle " + dateFormatter.format(Calendar.getInstance().getTime()) + ".png";

        googleMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                endLayout.setDrawingCacheEnabled(true);
                Bitmap backBitmap = Bitmap.createBitmap(endLayout.getDrawingCache());
                endLayout.setDrawingCacheEnabled(false);

                Bitmap bmOverlay = Bitmap.createBitmap(
                        backBitmap.getWidth(), backBitmap.getHeight(),
                        backBitmap.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(snapshot, new Matrix(), null);
                canvas.drawBitmap(backBitmap, 0, 0, null);

                storeAndShare(bmOverlay, fileName);
            }
        });
    }

    //endregion

    //region public

    public void storeAndShare(Bitmap bm, String fileName) {
        final String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Screenshots";
        File dir = new File(dirPath);
        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dirPath, fileName);
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        shareImage(file);
    }

    private void shareImage(File file) {
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/png");

        intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            startActivity(Intent.createChooser(intent, "Share Screenshot"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getBaseContext(), "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

    //endregion
}
