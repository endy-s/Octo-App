package com.br.octo.board.modules;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import com.br.octo.board.Constants;
import com.br.octo.board.R;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.main.MainActivity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;


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
            createSplashErrorDialog(R.string.bt_error_title, R.string.ble_not_supported);
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            createSplashErrorDialog(R.string.bt_error_title, R.string.bt_not_available);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
        } else {
            checkPermissions();
        }
    }

    //endregion

    //region Results

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                checkPermissions();
            } else {
                createSplashErrorDialog(R.string.bt_error_title, R.string.bt_not_enabled_leaving);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_LOCATION: {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    callMainActivity();
                } else {
                    createDialog(R.string.permission_error_title, R.string.permission_error_msg)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
        }
    }

    //endregion

    //region Private

    private void getScreenOnPreference() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_keep_screen), false)) {
            BaseActivity.keepScreen = true;
            AppCompatPreferenceActivity.keepScreen = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void checkPermissions() {
        if (mBluetoothAdapter.getBluetoothLeScanner() == null) {
            createSplashErrorDialog(R.string.bt_error_title, R.string.ble_not_supported);
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
                callMainActivity();
            }
        }
    }

    private void callMainActivity() {
        int SPLASH_TIME_OUT = 1000;

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
                .setPositiveButton(android.R.string.ok, this)
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
