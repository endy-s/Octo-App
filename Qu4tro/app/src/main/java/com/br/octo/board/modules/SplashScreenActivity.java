package com.br.octo.board.modules;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import com.br.octo.board.R;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.main.MainActivity;


public class SplashScreenActivity extends BaseActivity implements AlertDialog.OnClickListener,
        AlertDialog.OnDismissListener {

    // Future: Add download of user data (if logged in)

    //Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getScreenOnPreference();

        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            createSplashErrorDialog(getString(R.string.bt_error_title), getString(R.string.ble_not_supported));
        }

        // Initializes a Bluetooth adapter.
        // For API level 18 and above, get the reference through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            createSplashErrorDialog(getString(R.string.bt_error_title), getString(R.string.bt_not_available));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            startMainActivity();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                startMainActivity();
            } else {
                createSplashErrorDialog(getString(R.string.bt_error_title), getString(R.string.bt_not_enabled_leaving));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void getScreenOnPreference() {
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_keep_screen), false)) {
            BaseActivity.keepScreen = true;
            AppCompatPreferenceActivity.keepScreen = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void startMainActivity() {
        if (mBluetoothAdapter.getBluetoothLeScanner() == null) {
            createSplashErrorDialog(getString(R.string.bt_error_title), getString(R.string.ble_not_supported));
        } else {
            startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
            overridePendingTransition(R.anim.main_in, R.anim.splash_out);
            finish();
        }
    }

    private void createSplashErrorDialog(String title, String message) {
        createDialog(title, message)
                .setPositiveButton(android.R.string.ok, this)
                .setOnDismissListener(this)
                .create()
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
