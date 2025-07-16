package com.bridger.ui.scanner;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.bridger.BleScannerManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.schedulers.Schedulers;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ScannerViewModel extends AndroidViewModel {
    private static final String TAG = "ScannerViewModel";
    private static final long DEVICE_REMOVE_TIMEOUT = 30; // Timeout for device removal (seconds)
    private static final long DEVICE_UPDATE_THROTTLE_MS = 500; // Timeout for smoothing RSSI updates (milliseconds)

    private final BleScannerManager bleScannerManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // BehaviorSubject for storing and emitting the list of devices
    private final BehaviorSubject<List<DeviceScanResult>> devicesSubject = BehaviorSubject.createDefault(Collections.emptyList());

    public ScannerViewModel(@NonNull Application application) {
        super(application);
        bleScannerManager = new BleScannerManager();
    }

    // Method that Activity will call to get the device stream
    public Observable<List<DeviceScanResult>> getDevicesStream() {
        return devicesSubject.hide(); // hide() to prevent external emissions
    }

    // Public method to start scanning, called from Activity
    public void startScan() {
        // Get the hot scan stream from BleScannerManager
        Observable<ScanResult> scanResultsStream = bleScannerManager.getScanStream();

        // Create a single stream of actions (Upsert and Remove)
        Observable<ScannerAction> actionsStream = createDeviceActionsStream(scanResultsStream);

        // Use scan to manage the list state based on the action stream
        disposables.add(
                actionsStream
                        .scan(Collections.<String, DeviceScanResult>emptyMap(), this::onScan) // Accumulate a Map
                        .map(this::mapToListAndSort) // Convert Map to List and sort
                        .observeOn(AndroidSchedulers.mainThread()) // Update UI on the main thread
                        .doOnSubscribe(disposable -> devicesSubject.onNext(Collections.emptyList())) // Clear list on subscribe
                        .subscribe(
                                devicesSubject::onNext, // Emit the new list to the Subject
                                this::onScanFailed
                        )
        );
    }

    /**
     * Creates a stream of actions (add/update and remove) based on scan results.
     * Uses groupBy, publish, throttleFirst, and debounce to manage device lifecycle.
     */
    private Observable<ScannerAction> createDeviceActionsStream(Observable<ScanResult> scanResultsStream) {
        return scanResultsStream
                .groupBy(scanResult -> scanResult.getDevice().getAddress()) // Group by device address
                .flatMap(groupedObservable -> groupedObservable.publish(sharedGroup -> {
                    // 'sharedGroup' is now a "multi-user" version
                    // of the stream for a single group. It can have multiple subscribers.

                    // Stream for updates (with throttling)
                    Observable<ScannerAction> upsertActions = sharedGroup
                            .throttleFirst(DEVICE_UPDATE_THROTTLE_MS, TimeUnit.MILLISECONDS, Schedulers.computation()) // Emit no more often than every half-second
                            .map(DeviceUpsertAction::new);

                    // Stream for removal (with a long delay)
                    Observable<ScannerAction> removeActions = sharedGroup
                            .debounce(DEVICE_REMOVE_TIMEOUT, TimeUnit.SECONDS, Schedulers.computation()) // Fires if device is silent for DEVICE_REMOVE_TIMEOUT seconds
                            .map(scanResult -> new DeviceRemoveAction(scanResult.getDevice().getAddress()));

                    // Merge them into a single action stream for this group
                    return Observable.merge(upsertActions, removeActions)
                            .subscribeOn(Schedulers.computation()); // Process on computation scheduler
                }));
    }

    /**
     * Reducer for the scan operator. Updates the Map of devices based on the received action.
     */
    private Map<String, DeviceScanResult> onScan(Map<String, DeviceScanResult> currentMap, ScannerAction action) {
        Map<String, DeviceScanResult> newMap = new HashMap<>(currentMap); // Create a mutable copy of the Map

        if (action instanceof DeviceUpsertAction upsertAction) {
            DeviceScanResult newDevice = new DeviceScanResult(upsertAction.getScanResult());
            newMap.put(newDevice.getAddress(), newDevice); // Add or update device in Map
        } else if (action instanceof DeviceRemoveAction removeAction) {
            newMap.remove(removeAction.getDeviceAddress()); // Remove device from Map
        }
        return Collections.unmodifiableMap(newMap); // Return an unmodifiable Map
    }

    /**
     * Converts a Map of devices to a sorted List.
     */
    private List<DeviceScanResult> mapToListAndSort(Map<String, DeviceScanResult> deviceMap) {
        List<DeviceScanResult> newList = new ArrayList<>(deviceMap.values());
        // Sort the list by RSSI in descending order (strongest signal at the top)
        newList.sort((d1, d2) -> Integer.compare(d2.getRssi(), d1.getRssi()));
        return Collections.unmodifiableList(newList);
    }

    private void onScanFailed(Throwable error) {
        Log.e(TAG, "Scan chain failed: ", error);
        devicesSubject.onNext(Collections.emptyList()); // Clear list on error
        Toast.makeText(getApplication(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        Log.d(TAG, "ScannerViewModel cleared, all disposables cleared.");
    }
}
