package com.bridger.ui.scanner;

import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.bridger.ui.connection.ConnectionActivity; // Import ConnectionActivity

public class DeviceListAdapter extends ListAdapter<DeviceScanResult, DeviceListViewHolder> {

    private final Context context;
    public static final String EXTRA_DEVICE_ADDRESS = "com.bridger.EXTRA_DEVICE_ADDRESS"; // Define extra key

    public DeviceListAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
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
        holder.itemView.setOnClickListener(v -> {
            // Navigate directly to ConnectionActivity
            Intent intent = new Intent(context, ConnectionActivity.class);
            // Pass only the device address (String)
            intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
            context.startActivity(intent);
        });
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
