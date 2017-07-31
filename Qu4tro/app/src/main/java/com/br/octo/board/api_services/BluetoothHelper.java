package com.br.octo.board.api_services;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.br.octo.board.R;

import java.util.UUID;

import static com.br.octo.board.Constants.BT_CONNECTION_TIME_OUT;

/**
 * Created by Endy on 25/05/2017.
 */

public class BluetoothHelper {
    private static final BluetoothHelper btInstance = new BluetoothHelper();

    public static BluetoothHelper getInstance() {
        return btInstance;
    }

    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic characteristicRxTx;
    private SharedPreferences sharedPref;
    private Context context;
    private Resources resources;

    private BluetoothCallback callback;
    private boolean btConnected = false;
    private Handler connectionErrorHandler;
    private Runnable connectionErrorRunnable;

    public void setCallback(BluetoothCallback callback) {
        this.callback = callback;
    }

    public void connectToDevice(Context context, BluetoothDevice device) {
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        resources = context.getResources();

        // Check if was Previously connected to the device
        if (mGatt != null) {
            if (mGatt.getDevice() == device) {
                Log.d("BLUETOOTH", "PREVIOUSLY CONNECTED TO THIS DEVICE");
                if (mGatt.connect()) {
                    mGatt.discoverServices();
                    return;
                }
            }
        }

        btConnected = false;

        mGatt = device.connectGatt(context, true, gattCallback);

        connectionErrorRunnable = new Runnable() {
            @Override
            public void run() {
                mGatt.disconnect();
                callback.onDeviceDisconnected();
            }
        };

        connectionErrorHandler = new Handler();
        connectionErrorHandler.postDelayed(connectionErrorRunnable, BT_CONNECTION_TIME_OUT);
    }

    public void disconnect() {
        if (btConnected) {
            sendMessage("<D>");
        }
        mGatt.disconnect();
        callback.onDeviceDisconnected();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    mGatt.close();
                    mGatt = null;
                    btConnected = false;
                    connectionErrorHandler.removeCallbacks(connectionErrorRunnable);
                    callback.onDeviceDisconnected();
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            gatt.readCharacteristic(gatt.getService(UUID.fromString(SampleGattAttributes.HM_10_SERVICE_TO_READ)).getCharacteristic(UUID.fromString(SampleGattAttributes.HM_10_CHARACTERISTICS_TO_READ)));
            characteristicRxTx = gatt.getService(UUID.fromString(SampleGattAttributes.HM_10_CONF)).getCharacteristic(UUID.fromString(SampleGattAttributes.HM_RX_TX));

            gatt.setCharacteristicNotification(characteristicRxTx, true);

            sendHandshake();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String answer = characteristic.getStringValue(0);
            Log.d("RECEIVED", "This characteristic:" + answer);

            if (!btConnected) {
                if (answer.matches("<BOARD>")) {
                    sendLightState();
                    callback.onDeviceConnected();
                    btConnected = true;
                } else {
                    disconnect();
                }
            } else {
                callback.onMessageReceived(answer.replaceAll("[<> ]", ""));

                if (answer.startsWith("<B")) {
                    sendMessage("<OK>");
                } else if (answer.startsWith("<U")) {
                    if (answer.contains("L=")) {
                        updateLightState(Integer.valueOf(answer.split(";")[1].substring(2)));
                    }
                    sendMessage("<OK>");
                } else if (answer.matches("<OK>")) {
                    //
                }
            }
        }
    };

    public boolean getConnectionStatus() {
        return btConnected;
    }

    private void sendHandshake() {
        connectionErrorHandler.removeCallbacks(connectionErrorRunnable);
        sendMessage("<OCTO>");
    }

    private void sendLightState() {
        String initialString = "<W=3;", endingString = ";>";
        String lightMode = "L=", lightFreq = ";F=", lightInt = ";I=";

        if (sharedPref.getBoolean(resources.getString(R.string.pref_key_light_enabled), false)) {
            lightMode += sharedPref.getString(resources.getString(R.string.pref_key_light_mode), "0");
        } else {
            lightMode += "0";
        }

        lightFreq += sharedPref.getString(resources.getString(R.string.pref_key_light_freq), "0");

        int tempInt = sharedPref.getInt(resources.getString(R.string.pref_key_light_intensity), 50);
        lightInt += tempInt == 100 ? 99 : tempInt;

        sendMessage(initialString + lightMode + lightFreq + lightInt + endingString);
    }

    public void sendBcapChangedState() {
        String bcapChangeMsg = "<C=";

        if (sharedPref.getBoolean(resources.getString(R.string.pref_key_bcap_enable), false)) {
            bcapChangeMsg += "1;>";
        } else {
            bcapChangeMsg += "0;>";
        }

        sendMessage(bcapChangeMsg);
    }

    private void updateLightState(int newState) {
        SharedPreferences.Editor prefEditor = sharedPref.edit();

//        Constants.updateSettingsScreen = true;

        if (newState == 0) {
            prefEditor.putBoolean(resources.getString(R.string.pref_key_light_enabled), false);
        } else {
            prefEditor.putBoolean(resources.getString(R.string.pref_key_light_enabled), true);
            prefEditor.putString(resources.getString(R.string.pref_key_light_mode), String.valueOf(newState));
        }

        prefEditor.apply();
    }

    public void sendMessage(String message) {
        final byte[] tx = message.getBytes();
        characteristicRxTx.setValue(tx);
        characteristicRxTx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mGatt.writeCharacteristic(characteristicRxTx);
        Log.d("SENT", "This message: " + message);
    }

    public interface BluetoothCallback {
        void onMessageReceived(String message);

        void onDeviceConnected();

        void onDeviceDisconnected();
    }
}