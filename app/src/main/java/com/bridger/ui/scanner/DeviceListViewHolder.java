package com.bridger.ui.scanner;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bridger.databinding.ListItemDeviceDetailedBinding;

public class DeviceListViewHolder extends RecyclerView.ViewHolder {

    private final ListItemDeviceDetailedBinding binding;

    public DeviceListViewHolder(@NonNull ListItemDeviceDetailedBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(DeviceScanResult device) {
        binding.deviceName.setText(device.getName());
        binding.deviceAddress.setText(device.getAddress());
        binding.deviceRssi.setText("RSSI: " + device.getRssi() + " dBm");
    }

    public static DeviceListViewHolder create(ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ListItemDeviceDetailedBinding binding = ListItemDeviceDetailedBinding.inflate(layoutInflater, parent, false);
        return new DeviceListViewHolder(binding);
    }
}
