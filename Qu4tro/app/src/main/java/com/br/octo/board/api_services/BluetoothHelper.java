package com.br.octo.board.api_services;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Created by Endy on 25/05/2017.
 */

public class BluetoothHelper {
    private static final BluetoothHelper btInstance = new BluetoothHelper();

    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic characteristicRxTx;

    private boolean btConnected = false;

    public static BluetoothHelper getInstance() {
        return btInstance;
    }

    private BluetoothCallback callback;

    public void setCallback(BluetoothCallback callback) {
        this.callback = callback;
    }

    public void connectToDevice(Context context, BluetoothDevice device){
        // Check if was Previously connected to the device
        if (mGatt != null) {
            if (mGatt.getDevice() == device) {
                Log.d("BLUETOOTH", "PREVIOUSLY CONNECTED TO THIS DEVICE");
                mGatt.connect();
                return;
            }
        }

        btConnected = false;
        // TODO: Check how the autoConnect works and if it fits the needs of the app
        mGatt = device.connectGatt(context, false, gattCallback);
    }

    public void disconnect() {
        mGatt.disconnect();
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
                    btConnected = false;
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));

//            BluetoothGattService mCustomService = gatt.
//                    getService(UUID.fromString(SampleGattAttributes.HM_RX_TX));

            characteristicRxTx = services.get(3).
                    getCharacteristic(UUID.fromString(SampleGattAttributes.HM_RX_TX));

            Log.d("BLUETOOTH", "The right service is: " + services.get(3).getUuid().toString());

            gatt.setCharacteristicNotification(characteristicRxTx, true);

            sendHandshake();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String answer = characteristic.getStringValue(0);
            Log.d("RECEIVED", "This characteristic:" + answer);

            if (!btConnected) {
                if (answer.matches("<BOARD>")) {
                    callback.onDeviceConnected();
                    btConnected = true;
                }
            }
            else {

                callback.onMessageReceived(answer);

                if (answer.matches("<OK>")) {
                    //TODO
                }
                else if (answer.startsWith("<U;")) {
                    //TODO
                }
                else if (answer.startsWith("<B;")) {
                    //TODO

                }
            }
        }
    };

    public boolean getConnectionStatus () { return btConnected; }

    private void sendHandshake() { sendMessage("<OCTO>\n"); }

    public void sendMessage(String message) {
        final byte[] tx = message.getBytes();
        characteristicRxTx.setValue(tx);
        characteristicRxTx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mGatt.writeCharacteristic(characteristicRxTx);
    }

    public interface BluetoothCallback {
        void onMessageReceived(String message);
        void onDeviceConnected();
    }
}



