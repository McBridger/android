package com.bridger;

import android.bluetooth.BluetoothAdapter; // Import BluetoothAdapter
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bridger.constants.Constants;
import com.bridger.events.ClipboardEvent;
import com.bridger.model.Characteristic;
import com.bridger.model.ConnectionState;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class BleConnectionManager {

    private static final String TAG = "BleConnectionManager";
    private static BleConnectionManager instance;
    private final BridgerBleManager bleManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Store store;

    private BleConnectionManager(@NonNull Context context, @NonNull Store store) {
        this.store = store;
        this.bleManager = new BridgerBleManager(context.getApplicationContext());
        bleManager.setConnectionObserver(new ConnectionObserver() {
            @Override
            public void onDeviceConnecting(@NonNull BluetoothDevice device) {
                store.connection.onNext(ConnectionState.CONNECTING);
            }

            @Override
            public void onDeviceConnected(@NonNull BluetoothDevice device) {
                // Intermediate state, wait for onDeviceReady
            }

            @Override
            public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
                store.connection.onNext(ConnectionState.FAILED);
            }

            @Override
            public void onDeviceReady(@NonNull BluetoothDevice device) {
                store.connection.onNext(ConnectionState.CONNECTED);
            }

            @Override
            public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
                store.connection.onNext(ConnectionState.DISCONNECTING);
            }

            @Override
            public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
                store.connection.onNext(ConnectionState.DISCONNECTED);
            }
        });

        // Subscribe to clipboard events from the Store to handle outgoing data
        disposables.add(store.clipboard
                .filter(event -> event.getType() == ClipboardEvent.EventType.SEND_REQUESTED)
                .map(ClipboardEvent::getData) // Get text data from event
                .flatMapCompletable(text -> {
                    if (text != null) {
                        Data data = new Data(text.getBytes());
                        return bleManager.performWriteCharacteristic(data)
                                .doOnComplete(() -> store.lastAction.onNext("Sent: " + text)); // Update last action on success
                    }
                    return Completable.complete(); // No text to send
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Log.d(TAG, "Clipboard data sent via Store subscription and last action updated."),
                        throwable -> Log.e(TAG, "Failed to send clipboard data: " + throwable.getMessage())
                ));

        // Subscribe to CONNECT_REQUESTED events from the Store to initiate connection
        disposables.add(store.clipboard
                .filter(event -> event.getType() == ClipboardEvent.EventType.CONNECT_REQUESTED)
                .map(ClipboardEvent::getData) // Get device address
                .subscribeOn(Schedulers.io())
                .subscribe(deviceAddress -> {
                    if (deviceAddress != null) {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter != null) {
                            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                            if (device != null) {
                                connect(device); // Call the connect method
                            } else {
                                Log.e(TAG, "BluetoothDevice not found for address: " + deviceAddress);
                                store.connection.onNext(ConnectionState.FAILED);
                            }
                        } else {
                            Log.e(TAG, "BluetoothAdapter not available.");
                            store.connection.onNext(ConnectionState.FAILED);
                        }
                    }
                }, throwable -> Log.e(TAG, "Error observing CONNECT_REQUESTED: " + throwable.getMessage())));

        // Subscribe to DISCONNECT_REQUESTED events from the Store
        disposables.add(store.clipboard
                .filter(event -> event.getType() == ClipboardEvent.EventType.DISCONNECT_REQUESTED)
                .subscribeOn(Schedulers.io())
                .subscribe(event -> {
                    Log.d(TAG, "Disconnect requested.");
                    disconnect();
                }, throwable -> Log.e(TAG, "Error observing DISCONNECT_REQUESTED: " + throwable.getMessage())));
    }

    public static synchronized BleConnectionManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new BleConnectionManager(context.getApplicationContext(), Store.getInstance());
        }
        return instance;
    }

    // --- Public API ---

    @Nullable
    public BluetoothDevice getConnectedDevice() {
        return bleManager.getBluetoothDevice();
    }

    public void connect(@NonNull BluetoothDevice device) {
        if (store.connection.getValue() == ConnectionState.CONNECTED ||
            store.connection.getValue() == ConnectionState.CONNECTING) {
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

    // The inner class that extends BleManager and can access protected methods
    private class BridgerBleManager extends BleManager {

        private final Map<UUID, Characteristic> SUPPORTED_CHARACTERISTICS = new HashMap<>();

        {
            SUPPORTED_CHARACTERISTICS.put(Constants.ANDROID_TO_MAC_CHARACTERISTIC_UUID, new Characteristic(Constants.ANDROID_TO_MAC_CHARACTERISTIC_UUID));
            SUPPORTED_CHARACTERISTICS.put(Constants.MAC_TO_ANDROID_CHARACTERISTIC_UUID, new Characteristic(Constants.MAC_TO_ANDROID_CHARACTERISTIC_UUID, data -> {
                String value = data.getStringValue(0);
                if (value == null) return;
                store.clipboard.onNext(ClipboardEvent.createReceiveEvent(value));
            }));
        }

        public BridgerBleManager(@NonNull Context context) {
            super(context);
        }

        @Override
        public void log(int priority, @NonNull String message) {
            Log.println(priority, TAG, message);
        }

        public Completable performWriteCharacteristic(@NonNull Data data) {
            return Completable.create(emitter -> {
                Characteristic androidToMacChar = SUPPORTED_CHARACTERISTICS.get(Constants.ANDROID_TO_MAC_CHARACTERISTIC_UUID);

                if (androidToMacChar == null || androidToMacChar.gattCharacteristic == null) {
                    emitter.onError(new Throwable("Characteristic not found: " + Constants.ANDROID_TO_MAC_CHARACTERISTIC_UUID));
                    return;
                }
                writeCharacteristic(androidToMacChar.gattCharacteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        .split()
                        .done(device -> emitter.onComplete())
                        .fail((device, status) -> emitter.onError(new Throwable("Failed to write characteristic with status: " + status)))
                        .enqueue();
            });
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(Constants.BRIDGER_SERVICE_UUID);

            if (service != null) {
                for (Characteristic characteristic : SUPPORTED_CHARACTERISTICS.values()) {
                    BluetoothGattCharacteristic gattChar = service.getCharacteristic(characteristic.uuid);
                    if (gattChar != null) {
                        characteristic.gattCharacteristic = gattChar;
                    }
                }
            }

            return true;
        }

        @Override
        protected void initialize() {
            for (Characteristic characteristic : SUPPORTED_CHARACTERISTICS.values()) {
                if (characteristic.notificationCallback == null) continue;
                if (characteristic.gattCharacteristic == null) {
                    log(Log.WARN, "Characteristic " + characteristic.uuid + " not found on device. Skipping initialization.");
                    continue;
                }

                setNotificationCallback(characteristic.gattCharacteristic)
                    .with((device, data) -> characteristic.notificationCallback.accept(data));

                beginAtomicRequestQueue()
                    .add(enableNotifications(characteristic.gattCharacteristic)
                            .fail((device, status) -> log(Log.ERROR, "Could not enable notifications for " + characteristic.uuid + ": " + status))
                    )
                    .enqueue();
            }
        }

        @Override
        protected void onServicesInvalidated() {
            Log.w(TAG, "Services Invalidated");
            for (Characteristic characteristic : SUPPORTED_CHARACTERISTICS.values()) {
                characteristic.gattCharacteristic = null;
            }
        }
    }
}
