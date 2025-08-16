package com.bridger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.bridger.constants.Constants;
import com.bridger.events.ClipboardEvent;
import com.bridger.model.Characteristic;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class BleConnectionManager {

    private static final String TAG = "BleConnectionManager";
    private static BleConnectionManager instance;
    private final BridgerBleManager bleManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final BehaviorSubject<ConnectionState> connectionStateSubject = BehaviorSubject.createDefault(ConnectionState.DISCONNECTED);
    private final BehaviorSubject<List<BluetoothGattService>> servicesSubject = BehaviorSubject.create();
    private final BehaviorSubject<Throwable> errorSubject = BehaviorSubject.create();
    public final PublishSubject<ClipboardEvent> clipboardEvents = PublishSubject.create();

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        READY,
        INITIALIZING
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
                connectionStateSubject.onNext(ConnectionState.READY);
            }

            @Override
            public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
                connectionStateSubject.onNext(ConnectionState.DISCONNECTING);
            }

            @Override
            public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
                connectionStateSubject.onNext(ConnectionState.DISCONNECTED);
                servicesSubject.onNext(List.of());
            }
        });

        // Subscribe to clipboardEvents to handle outgoing clipboard data
        disposables.add(clipboardEvents
                .filter(event -> event instanceof ClipboardEvent.Send)
                .cast(ClipboardEvent.Send.class)
                .flatMapCompletable(sendEvent -> {
                    Data data = new Data(sendEvent.text.getBytes());
                    return bleManager.performWriteCharacteristic(data);
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Log.d(TAG, "Clipboard data sent via internal subscription"),
                        throwable -> errorSubject.onNext(new Throwable("Failed to send clipboard data: " + throwable.getMessage()))
                ));
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

    // The inner class that extends BleManager and can access protected methods
    private class BridgerBleManager extends BleManager {

        // Define all supported characteristics with their UUIDs and notification callbacks
        private final Map<UUID, Characteristic> SUPPORTED_CHARACTERISTICS = new HashMap<>();

        {
            // Android to Mac Characteristic (Write)
            SUPPORTED_CHARACTERISTICS.put(Constants.ANDROID_TO_MAC_CHARACTERISTIC_UUID, new Characteristic(Constants.ANDROID_TO_MAC_CHARACTERISTIC_UUID));

            // Mac to Android Characteristic (Notify) - Dummy callback for now, as requested
            SUPPORTED_CHARACTERISTICS.put(Constants.MAC_TO_ANDROID_CHARACTERISTIC_UUID, new Characteristic(Constants.MAC_TO_ANDROID_CHARACTERISTIC_UUID, data -> {
                String value = data.getStringValue(0);
                if (value == null) return;
                clipboardEvents.onNext(new ClipboardEvent.Receive(value));
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
            servicesSubject.onNext(gatt.getServices());
            final BluetoothGattService service = gatt.getService(Constants.BRIDGER_SERVICE_UUID);

            if (service != null) {
                for (Characteristic characteristic : SUPPORTED_CHARACTERISTICS.values()) {
                    BluetoothGattCharacteristic gattChar = service.getCharacteristic(characteristic.uuid);
                    if (gattChar != null) {
                        characteristic.gattCharacteristic = gattChar;
                    }
                }
            }

            // Always return true to prevent auto-disconnect.
            // We will handle characteristic availability gracefully in initialize().
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
            servicesSubject.onNext(List.of());
        }
    }
}
