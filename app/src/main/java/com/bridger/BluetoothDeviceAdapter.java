package com.bridger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.ArrayList;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(ScanResult device);
    }

    private final List<ScanResult> devices;
    private OnDeviceClickListener listener;

    public BluetoothDeviceAdapter(List<ScanResult> devices) {
        this.devices = devices;
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        ScanResult device = devices.get(position);
        holder.deviceName.setText(device.getDevice().getName() != null ? device.getDevice().getName() : "Unknown Device");
        holder.deviceAddress.setText(device.getDevice().getAddress());
        holder.deviceRssi.setText("RSSI: " + device.getRssi() + " dBm");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void addDevice(ScanResult newDevice) {
        boolean found = false;
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDevice().getAddress().equals(newDevice.getDevice().getAddress())) {
                devices.set(i, newDevice);
                notifyItemChanged(i);
                found = true;
                break;
            }
        }
        if (!found) {
            devices.add(newDevice);
            notifyItemInserted(devices.size() - 1);
        }
    }

    public void clearDevices() {
        devices.clear();
        notifyDataSetChanged();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceName;
        final TextView deviceAddress;
        final TextView deviceRssi;

        DeviceViewHolder(View view) {
            super(view);
            deviceName = view.findViewById(R.id.device_name);
            deviceAddress = view.findViewById(R.id.device_address);
            deviceRssi = view.findViewById(R.id.device_rssi);
        }
    }
}
