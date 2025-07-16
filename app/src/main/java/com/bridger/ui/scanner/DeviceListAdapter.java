package com.bridger.ui.scanner;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class DeviceListAdapter extends ListAdapter<DeviceScanResult, DeviceListViewHolder> {

    public DeviceListAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public DeviceListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return DeviceListViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListViewHolder holder, int position) {
        DeviceScanResult device = getItem(position);
        holder.bind(device);
    }

    private static final DiffUtil.ItemCallback<DeviceScanResult> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DeviceScanResult>() {
                @Override
                public boolean areItemsTheSame(@NonNull DeviceScanResult oldItem, @NonNull DeviceScanResult newItem) {
                    // Compare by unique identifier (MAC address)
                    return oldItem.getAddress().equals(newItem.getAddress());
                }

                @Override
                public boolean areContentsTheSame(@NonNull DeviceScanResult oldItem, @NonNull DeviceScanResult newItem) {
                    // Compare contents to determine if the item needs to be redrawn
                    return oldItem.equals(newItem);
                }
            };
}
