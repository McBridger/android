package com.bridger.ui.scanner;

import android.os.ParcelUuid;
import com.bridger.constants.Constants;

import java.util.List;
import java.util.Objects;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class DeviceScanResult {
    private final String name;
    private final String address;
    private int rssi; // Not final anymore, as it will be updated
    private long lastSeen; // Add a field for the last seen timestamp
    private final ScanResult rawScanResult; // Keep it final, as this is the original scan result
    private final boolean isBridgerDevice; // New field to indicate if it's a Bridger device

    public DeviceScanResult(ScanResult scanResult) {
        this.rawScanResult = scanResult;
        this.address = scanResult.getDevice().getAddress();
        this.rssi = scanResult.getRssi();
        this.name = scanResult.getScanRecord() != null ? scanResult.getScanRecord().getDeviceName() : "Unknown Device";
        this.lastSeen = System.currentTimeMillis(); // Initialize on creation
        this.isBridgerDevice = checkIsBridger(scanResult); // Determine Bridger status on creation
    }

    // Private constructor for creating updated copies
    private DeviceScanResult(String name, String address, int rssi, long lastSeen, ScanResult rawScanResult, boolean isBridgerDevice) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
        this.lastSeen = lastSeen;
        this.rawScanResult = rawScanResult;
        this.isBridgerDevice = isBridgerDevice; // Pass the flag
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getRssi() {
        return rssi;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public ScanResult getRawScanResult() {
        return rawScanResult;
    }

    public boolean isBridgerDevice() {
        return isBridgerDevice;
    }

    // Method to create a new copy with updated RSSI and lastSeen
    public DeviceScanResult withUpdatedRssi(int newRssi) {
        return new DeviceScanResult(this.name, this.address, newRssi, System.currentTimeMillis(), this.rawScanResult, this.isBridgerDevice);
    }

    // Helper method to check if the scan result contains the Bridger service UUID
    private boolean checkIsBridger(ScanResult rawScanResult) {
        if (rawScanResult == null || rawScanResult.getScanRecord() == null) {
            return false;
        }
        List<ParcelUuid> serviceUuids = rawScanResult.getScanRecord().getServiceUuids();
        if (serviceUuids == null) {
            return false;
        }
        return serviceUuids.contains(new ParcelUuid(Constants.BRIDGER_SERVICE_UUID));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceScanResult that = (DeviceScanResult) o;
        // For DiffUtil.ItemCallback.areContentsTheSame(), compare all displayed fields:
        // RSSI, name (null-safe), address (as the main identifier), and isBridgerDevice
        return rssi == that.rssi &&
               isBridgerDevice == that.isBridgerDevice && // Include in equals
               Objects.equals(name, that.name) &&
               address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, rssi, isBridgerDevice); // Hash code must include all fields used in equals
    }
}
