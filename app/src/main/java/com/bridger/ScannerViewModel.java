package com.bridger;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import android.os.ParcelUuid;

public class ScannerViewModel extends AndroidViewModel {

    private static final String TAG = "ScannerViewModel";

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScannerCompat scanner;

    private final MutableLiveData<List<ScanResult>> _devices = new MutableLiveData<>();
    public LiveData<List<ScanResult>> getDevices() {
        return _devices;
    }

    private final MutableLiveData<Boolean> _scanning = new MutableLiveData<>();
    public LiveData<Boolean> getScanning() {
        return _scanning;
    }

    private final MutableLiveData<Boolean> _bluetoothEnabled = new MutableLiveData<>();
    public LiveData<Boolean> getBluetoothEnabled() {
        return _bluetoothEnabled;
    }

    private final MutableLiveData<Boolean> _locationEnabled = new MutableLiveData<>();
    public LiveData<Boolean> getLocationEnabled() {
        return _locationEnabled;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            List<ScanResult> currentDevices = _devices.getValue();
            if (currentDevices == null) {
                currentDevices = new ArrayList<>();
            } else {
                currentDevices = new ArrayList<>(currentDevices); // Create a mutable copy
            }

            boolean found = false;
            for (int i = 0; i < currentDevices.size(); i++) {
                if (currentDevices.get(i).getDevice().getAddress().equals(result.getDevice().getAddress())) {
                    currentDevices.set(i, result);
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentDevices.add(result);
            }
            Collections.sort(currentDevices, (d1, d2) -> Integer.compare(d2.getRssi(), d1.getRssi()));
            _devices.postValue(currentDevices);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            List<ScanResult> currentDevices = _devices.getValue();
            if (currentDevices == null) {
                currentDevices = new ArrayList<>();
            } else {
                currentDevices = new ArrayList<>(currentDevices); // Create a mutable copy
            }

            for (ScanResult newResult : results) {
                boolean found = false;
                for (int i = 0; i < currentDevices.size(); i++) {
                    if (currentDevices.get(i).getDevice().getAddress().equals(newResult.getDevice().getAddress())) {
                        currentDevices.set(i, newResult);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    currentDevices.add(newResult);
                }
            }
            Collections.sort(currentDevices, (d1, d2) -> Integer.compare(d2.getRssi(), d1.getRssi()));
            _devices.postValue(currentDevices);
        }

        @Override
        public void onScanFailed(int errorCode) {
            _scanning.postValue(false);
            Log.e(TAG, "Scan failed with error code: " + errorCode);
            // Handle scan failure, e.g., show a toast
        }
    };

    public ScannerViewModel(@NonNull Application application) {
        super(application);
        BluetoothManager bluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanner = BluetoothLeScannerCompat.getScanner();
    }

    public void startScan() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && isLocationEnabled(getApplication())) {
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();

            List<ScanFilter> filters = new ArrayList<>();
            // Add the service UUID filter
            UUID serviceUuid = UUID.fromString("81a936be-a052-4ef1-9c3c-073c0b63438d");
            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUuid)).build());

            scanner.startScan(filters, settings, scanCallback);
            _scanning.postValue(true);
        } else {
            checkBluetoothAndLocationState();
        }
    }

    public void stopScan() {
        scanner.stopScan(scanCallback);
        _scanning.postValue(false);
    }

    public void clearDevices() {
        _devices.postValue(new ArrayList<>());
    }

    public void checkBluetoothAndLocationState() {
        _bluetoothEnabled.postValue(bluetoothAdapter != null && bluetoothAdapter.isEnabled());
        _locationEnabled.postValue(isLocationEnabled(getApplication()));
    }

    private boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            try {
                int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
