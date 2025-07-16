package com.bridger.ui.scanner;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

// Base interface for all scanner actions
public interface ScannerAction {}

// Action: a device is found or updated
class DeviceUpsertAction implements ScannerAction {
    private final ScanResult scanResult;

    public DeviceUpsertAction(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }
}

// Action: a device should be removed
class DeviceRemoveAction implements ScannerAction {
    private final String deviceAddress;

    public DeviceRemoveAction(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }
}
