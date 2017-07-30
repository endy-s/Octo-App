package com.br.octo.board.modules;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;

import com.br.octo.board.Constants;
import com.br.octo.board.R;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.main.MainActivity;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.br.octo.board.Constants.REQUEST_CHECK_SETTINGS;
import static com.br.octo.board.Constants.REQUEST_ENABLE_BT;
import static com.br.octo.board.Constants.SPLASH_TIME_OUT;


public class SplashScreenActivity extends BaseActivity implements AlertDialog.OnClickListener,
        AlertDialog.OnDismissListener {

    //TODO - Future: Add download of user data (if logged in)

    //Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    //region lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getScreenOnPreference();

        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            createSplashErrorDialog(R.string.error_bt_error_title, R.string.error_ble_not_supported);
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            createSplashErrorDialog(R.string.error_bt_error_title, R.string.error_bt_not_available);
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            checkPermissions();
        }
    }

    //endregion

    //region Results

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                checkPermissions();
            } else {
                createDialog(R.string.error_bt_not_wanted_title, R.string.error_bt_not_wanted_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermissions();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        }
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                case RESULT_OK:
                    Log.e("Settings", "Result OK");
                    callMainActivity();
                    break;
                case RESULT_CANCELED:
                    Log.e("Settings", "Result Cancel");
                    createDialog(R.string.error_gps_not_enabled_title, R.string.error_gps_not_enabled_message)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    callMainActivity();
                                }
                            })
                            .setCancelable(false)
                            .show();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_LOCATION: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PERMISSION_GRANTED) {
                        showGPSRequestDialog();
                    } else {
                        createSplashErrorDialog(R.string.error_permission_error_title, R.string.error_permission_error_message);
                    }
                }
            }
        }
    }

    //endregion

    //region Private

    private void getScreenOnPreference() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean(getString(R.string.pref_key_keep_screen), false)) {
            BaseActivity.keepScreen = true;
            AppCompatPreferenceActivity.keepScreen = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putBoolean(getResources().getString(R.string.pref_key_bcap_enable), true);
        prefEditor.apply();
    }

    private void checkPermissions() {
        if (mBluetoothAdapter.getBluetoothLeScanner() == null) {
            createSplashErrorDialog(R.string.error_bt_error_title, R.string.error_ble_not_supported);
        } else {
            if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
//                createDialog(R.string.dialog_permission_request, R.string.dialog_permission_description)
//                        .setPositiveButton(R.string.dialog_next, null)
//                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                            @Override
//                            public void onDismiss(DialogInterface dialog) {
                ActivityCompat.requestPermissions(SplashScreenActivity.this,
                        new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION},
                        Constants.PERMISSION_REQUEST_LOCATION);
//                            }
//                        })
//                        .show();
            } else {
                showGPSRequestDialog();
            }
        }
    }

    private void showGPSRequestDialog() {
        generateGPSDialog().setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        callMainActivity();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(SplashScreenActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        createSplashErrorDialog(R.string.error_provider_location_title, R.string.error_provider_location_message);
                        break;
                }
            }
        });
    }

    private void callMainActivity() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));

                overridePendingTransition(R.anim.main_in, R.anim.splash_out);
                finish();
            }
        };
        new Handler().postDelayed(r, SPLASH_TIME_OUT);
    }

    private void createSplashErrorDialog(int titleID, int messageID) {
        createDialog(titleID, messageID)
                .setPositiveButton(R.string.ok, this)
                .setOnDismissListener(this)
                .show();
    }

    //endregion

    //region Error Dialog Listener

    @Override
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    //endregion
}
