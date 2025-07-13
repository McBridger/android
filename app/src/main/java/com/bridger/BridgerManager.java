package com.bridger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;

public class BridgerManager extends BleManager {

    private static final String TAG = "BridgerManager";

    // Service and Characteristic UUIDs
    private final static UUID SERVICE_UUID = UUID.fromString("81a936be-a052-4ef1-9c3c-073c0b63438d");
    private final static UUID CHARACTERISTIC_UUID = UUID.fromString("f95f7d8b-cd6d-433a-b1d1-28b0955faa52");

    private BluetoothGattCharacteristic pingCharacteristic;

    public BridgerManager(@NonNull final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new BridgerManagerGattCallback();
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class BridgerManagerGattCallback extends BleManagerGattCallback {

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                pingCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
            }
            return pingCharacteristic != null;
        }

        @Override
        protected void initialize() {
            // Set up notifications for the characteristic
            setNotificationCallback(pingCharacteristic)
                    .with((device, data) -> {
                        Log.d(TAG, "Received notification: " + data.getStringValue(0));
                        // Handle incoming data if needed
                    });
            enableNotifications(pingCharacteristic).enqueue();
        }
        
        @Override
        public void onServicesInvalidated() {
            // This method is called when the services are no longer valid (e.g., device disconnected)
            pingCharacteristic = null;
        }
    }

    public void sendPing(@NonNull final String message) {
        if (pingCharacteristic == null) {
            Log.e(TAG, "Ping characteristic not found. Cannot send message.");
            return;
        }

        final byte[] data = message.getBytes();
        writeCharacteristic(pingCharacteristic, data)
                .with((device, d) -> Log.d(TAG, "Ping sent: " + message))
                .fail((device, status) -> Log.e(TAG, "Failed to send ping: " + status))
                .enqueue();
    }
}
