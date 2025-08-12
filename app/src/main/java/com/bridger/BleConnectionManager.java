package com.bridger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class BleConnectionManager {

    private static final String TAG = "BleConnectionManager";
    private static BleConnectionManager instance;
    private final BridgerBleManager bleManager;

    // RxJava Subjects to emit state changes and data
    private final BehaviorSubject<ConnectionState> connectionStateSubject = BehaviorSubject.createDefault(ConnectionState.DISCONNECTED);
    private final BehaviorSubject<List<BluetoothGattService>> servicesSubject = BehaviorSubject.create();
    private final BehaviorSubject<Throwable> errorSubject = BehaviorSubject.create();

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    private BleConnectionManager(@NonNull Context context) {
        this.bleManager = new BridgerBleManager(context.getApplicationContext());
        bleManager.setConnectionObserver(new ConnectionObserver() {
            @Override
            public void onDeviceConnecting(@NonNull BluetoothDevice device) {
                connectionStateSubject.onNext(ConnectionState.CONNECTING);
            }

            @Override
            public void onDeviceConnected(@NonNull BluetoothDevice device) {
                // Intermediate state, wait for onDeviceReady
            }

            @Override
            public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
                connectionStateSubject.onNext(ConnectionState.DISCONNECTED);
                errorSubject.onNext(new Throwable("Failed to connect with reason: " + reason));
            }

            @Override
            public void onDeviceReady(@NonNull BluetoothDevice device) {
                connectionStateSubject.onNext(ConnectionState.CONNECTED);
            }

            @Override
            public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
                connectionStateSubject.onNext(ConnectionState.DISCONNECTING);
            }

            @Override
            public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
                connectionStateSubject.onNext(ConnectionState.DISCONNECTED);
                servicesSubject.onNext(List.of()); // Clear services on disconnect
            }
        });
    }

    public static synchronized BleConnectionManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new BleConnectionManager(context.getApplicationContext());
        }
        return instance;
    }

    // --- Public API ---

    public Observable<ConnectionState> getConnectionState() {
        return connectionStateSubject.hide();
    }

    public Observable<List<BluetoothGattService>> getDiscoveredServices() {
        return servicesSubject.hide();
    }

    public Observable<Throwable> getErrors() {
        return errorSubject.hide();
    }

    @Nullable
    public BluetoothDevice getConnectedDevice() {
        return bleManager.getBluetoothDevice();
    }

    public void connect(@NonNull BluetoothDevice device) {
        if (connectionStateSubject.getValue() == ConnectionState.CONNECTED || connectionStateSubject.getValue() == ConnectionState.CONNECTING) {
            Log.w(TAG, "Already connected or connecting to a device.");
            return;
        }
        bleManager.connect(device)
                .retry(3, 100)
                .useAutoConnect(true)
                .timeout(15_000)
                .enqueue();
    }

    public void disconnect() {
        bleManager.disconnect().enqueue();
    }

    // --- Characteristic Operations ---
    // These methods now delegate to the inner BleManager class using the new names

    public void enableNotifications(@NonNull BluetoothGattCharacteristic characteristic) {
        // Call the newly named method to avoid clashing
        bleManager.performEnableNotifications(characteristic);
    }

    public void readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {
        // Call the newly named method to avoid clashing
        bleManager.performReadCharacteristic(characteristic);
    }


    // The inner class that extends BleManager and can access protected methods
    private class BridgerBleManager extends BleManager {

        public BridgerBleManager(@NonNull Context context) {
            super(context);
        }

        // --- Helper methods with new names to avoid clashing with the superclass ---
        
        public void performEnableNotifications(@NonNull BluetoothGattCharacteristic characteristic) {
            setNotificationCallback(characteristic)
                    .with((device, data) -> {
                        Log.d(TAG, "Notification received from " + characteristic.getUuid() + ": " + data.getStringValue(0));
                        // TODO: Emit this data via an RxJava Subject if needed
                    });
            enableNotifications(characteristic).enqueue(); // This calls the superclass method
        }

        public void performReadCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {
            readCharacteristic(characteristic) // This calls the superclass method
                    .with((device, data) -> {
                        Log.d(TAG, "Characteristic " + characteristic.getUuid() + " read: " + data.getStringValue(0));
                        // TODO: Emit this data via an RxJava Subject if needed
                    })
                    .fail((device, status) -> {
                        errorSubject.onNext(new Throwable("Failed to read characteristic with status: " + status));
                    })
                    .enqueue();
        }

        @NonNull
        @Override
        protected BleManagerGattCallback getGattCallback() {
            return new BridgerBleManagerGattCallback();
        }

        private class BridgerBleManagerGattCallback extends BleManagerGattCallback {
            @Override
            protected void initialize() {
                // Called when the device is ready. Perform initial setup here.
            }

            @Override
            public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
                final List<BluetoothGattService> services = gatt.getServices();
                servicesSubject.onNext(services);
                return true;
            }

            @Override
            protected void onServicesInvalidated() {
                Log.w(TAG, "Services Invalidated");
                servicesSubject.onNext(List.of());
            }
        }
    }
}