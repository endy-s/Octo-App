package com.br.octo.board.modules;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.Toast;

import com.br.octo.board.R;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;
import com.br.octo.board.modules.base.BaseActivity;
import com.br.octo.board.modules.main.MainActivity;


public class SplashScreenActivity extends BaseActivity {

    //Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.
        // For API level 18 and above, get the reference through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(getBaseContext().BLUETOOTH_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
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
                // Bluetooth is now enabled, so set up a chat session
                startMainActivity();

                return;
            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, R.string.bt_not_enabled_leaving,
                        Toast.LENGTH_SHORT).show();
                finish();

                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startMainActivity()
    {
        // Delay fade for Splash Screen in background
        int SPLASH_TIME_OUT = 3000;

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));

                overridePendingTransition(R.anim.main_in, R.anim.splash_out);
                finish();
            }
        };

        new Handler().postDelayed(r, SPLASH_TIME_OUT);

        // "Keep Screen On" Preference state
        // Future: Add download of user data (if logged in)
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_keep_screen), false)) {
            BaseActivity.keepScreen = true;
            AppCompatPreferenceActivity.keepScreen = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
