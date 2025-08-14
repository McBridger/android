package com.bridger.ui.device;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.bridger.BleConnectionManager;
import com.bridger.constants.Constants;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

import static com.bridger.BleConnectionManager.ConnectionState; // Correct import for ConnectionState

public class DeviceViewModel extends AndroidViewModel {
    private static final String TAG = "DeviceViewModel";

    private final BleConnectionManager bleConnectionManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // Subject to hold the current connection state
    private final BehaviorSubject<ConnectionState> connectionStateSubject = BehaviorSubject.create();
    // Subject to hold the discovered services (as UUIDs for display)
    private final BehaviorSubject<List<UUID>> discoveredServicesSubject = BehaviorSubject.createDefault(List.of());
    // Subject to hold whether the Bridger service was found
    private final BehaviorSubject<Boolean> bridgerServiceFoundSubject = BehaviorSubject.createDefault(false);

    private BluetoothDevice connectedDevice; // Keep track of the currently connected device

    public DeviceViewModel(@NonNull Application application) {
        super(application);
        bleConnectionManager = BleConnectionManager.getInstance(application); // Get singleton instance

        // Observe connection state changes from BleConnectionManager
        disposables.add(bleConnectionManager.getConnectionState() // Correct method name
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        connectionStateSubject::onNext,
                        error -> Log.e(TAG, "Error observing connection state: " + error.getMessage())
                ));

        // Observe discovered services
        disposables.add(bleConnectionManager.getDiscoveredServices() // Correct method name
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        gattServices -> {
                            // Map BluetoothGattService objects to their UUIDs for simpler display
                            List<UUID> serviceUuids = gattServices.stream()
                                    .map(BluetoothGattService::getUuid)
                                    .collect(Collectors.toList());
                            discoveredServicesSubject.onNext(serviceUuids);

                            // Check if Bridger service is among discovered services
                            boolean foundBridger = serviceUuids.contains(Constants.BRIDGER_SERVICE_UUID);
                            bridgerServiceFoundSubject.onNext(foundBridger);
                        },
                        error -> Log.e(TAG, "Error observing discovered services: " + error.getMessage())
                ));
    }

    public Observable<ConnectionState> getConnectionStateStream() {
        return connectionStateSubject.hide();
    }

    public Observable<List<UUID>> getDiscoveredServicesStream() {
        return discoveredServicesSubject.hide();
    }

    public Observable<Boolean> getBridgerServiceFoundStream() {
        return bridgerServiceFoundSubject.hide();
    }

    public void connect(ScanResult scanResult) {
        if (scanResult == null) {
            Log.e(TAG, "ScanResult is null, cannot connect.");
            return;
        }
        connectedDevice = scanResult.getDevice();
        Log.d(TAG, "Attempting to connect to " + connectedDevice.getAddress());
        bleConnectionManager.connect(connectedDevice); // Pass BluetoothDevice
    }

    public void disconnect() {
        if (connectedDevice != null) {
            Log.d(TAG, "Attempting to disconnect from " + connectedDevice.getAddress());
            bleConnectionManager.disconnect(); // No arguments
            connectedDevice = null; // Clear connected device on disconnect
        } else {
            Log.d(TAG, "No device connected to disconnect from.");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        Log.d(TAG, "DeviceViewModel cleared, all disposables cleared.");
    }
}
