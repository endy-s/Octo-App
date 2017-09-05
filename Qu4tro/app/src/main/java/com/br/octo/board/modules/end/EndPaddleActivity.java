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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.br.octo.board.Constants;
import com.br.octo.board.R;
import com.br.octo.board.models.Paddle;
import com.br.octo.board.modules.base.BaseActivity;
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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class EndPaddleActivity extends BaseActivity {

    private GoogleMap googleMap;
    Paddle endedPaddle;

    //Widgets
    @BindView(R.id.endLayout)
    RelativeLayout endLayout;
    @BindView(R.id.endMap)
    MapView endMapView;
    @BindView(R.id.endShareButton)
    ImageView endShareButton;

    @BindView(R.id.endTxtKm)
    TextView endKm;
    @BindView(R.id.endTxtRows)
    TextView endRows;
    @BindView(R.id.endTxtTime)
    TextView endTime;
    @BindView(R.id.endTxtKcal)
    TextView endKcal;
    @BindView(R.id.endTxtSpeed)
    TextView endSpeed;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_end);
        ButterKnife.bind(this);

        //TODO check if need to show a progressDialog running while the map is not Ready

        if (getIntent().hasExtra(getString(R.string.paddle_extra))) {
            endedPaddle = Parcels.unwrap(getIntent().getParcelableExtra(getString(R.string.paddle_extra)));
            showPaddleInfo();
        } else {
            //TODO show error dialog
        }

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

                int numberPoints = endedPaddle.getTrack().size();

                if (numberPoints > 0) {
                    LatLng stop = new LatLng(endedPaddle.getTrack().get(numberPoints - 1).getLatitude(), endedPaddle.getTrack().get(numberPoints - 1).getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(stop).title("End").snippet("End of Paddling")).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));

                    PolylineOptions lineOptions = new PolylineOptions().width(5).color(Color.RED);
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                    for (int index = 0; index < numberPoints; index++) {
                        LatLng point = new LatLng(endedPaddle.getTrack().get(index).getLatitude(), endedPaddle.getTrack().get(index).getLongitude());
                        lineOptions.add(point);
                        builder.include(point);
                    }

                    Polyline line = googleMap.addPolyline(lineOptions);
                    LatLngBounds bounds = builder.build();

                    int padding = 25;
                    CameraUpdate zoom = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                    googleMap.moveCamera(zoom);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        endMapView.onResume();
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

    //region Results

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_STORAGE: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PERMISSION_GRANTED) {
                        shareClicked();
                    } else {
                        createDialog(R.string.error_permission_title, R.string.error_permission_storage_message)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                }
            }
        }
    }

    //endregion

    //region click listeners

    @OnClick(R.id.endShareButton)
    public void shareClicked() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EndPaddleActivity.this,
                    new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE},
                    Constants.PERMISSION_REQUEST_STORAGE);
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.share_loading), Toast.LENGTH_SHORT).show();

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
                    canvas.drawBitmap(backBitmap, 0, 0, null);
                    canvas.drawBitmap(snapshot, new Matrix(), null);

                    storeAndShare(bmOverlay, fileName);
                }
            });
        }
    }

    //endregion

    //region private

    private void showPaddleInfo() {
        endKm.setText(String.format("%.2f", endedPaddle.getDistance()));
        endRows.setText(String.format("%d", endedPaddle.getRows()));
        endKcal.setText(String.format("%d", endedPaddle.getKcal()));
        endSpeed.setText(String.format("%.2f", endedPaddle.getSpeed()));

        int hour = (int) endedPaddle.getDuration() / (60 * 60);
        int minutes = (int) (endedPaddle.getDuration() / 60) % 60;
//        int seconds = (int) endedPaddle.getDuration() % 60;
        endTime.setText(String.format("%02d:%02d", hour, minutes));
    }

    private void storeAndShare(Bitmap bm, String fileName) {
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
