package com.bridger.ui.scanner;

import java.util.Objects;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class DeviceScanResult {
    private final String name;
    private final String address;
    private int rssi; // Not final anymore, as it will be updated
    private long lastSeen; // Add a field for the last seen timestamp
    private final ScanResult rawScanResult; // Keep it final, as this is the original scan result

    public DeviceScanResult(ScanResult scanResult) {
        this.rawScanResult = scanResult;
        this.address = scanResult.getDevice().getAddress();
        this.rssi = scanResult.getRssi();
        this.name = scanResult.getScanRecord() != null ? scanResult.getScanRecord().getDeviceName() : "Unknown Device";
        this.lastSeen = System.currentTimeMillis(); // Initialize on creation
    }

    // Private constructor for creating updated copies
    private DeviceScanResult(String name, String address, int rssi, long lastSeen, ScanResult rawScanResult) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
        this.lastSeen = lastSeen;
        this.rawScanResult = rawScanResult;
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

    // Method to create a new copy with updated RSSI and lastSeen
    public DeviceScanResult withUpdatedRssi(int newRssi) {
        return new DeviceScanResult(this.name, this.address, newRssi, System.currentTimeMillis(), this.rawScanResult);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceScanResult that = (DeviceScanResult) o;
        // For DiffUtil.ItemCallback.areContentsTheSame(), compare all displayed fields:
        // RSSI, name (null-safe), and address (as the main identifier)
        return rssi == that.rssi &&
               Objects.equals(name, that.name) &&
               address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, rssi); // Hash code must include all fields used in equals
    }
}
