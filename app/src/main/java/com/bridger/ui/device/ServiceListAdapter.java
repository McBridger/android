package com.bridger.ui.device;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bridger.R;

import java.util.UUID;

public class ServiceListAdapter extends ListAdapter<UUID, ServiceListAdapter.ServiceViewHolder> {

    public ServiceListAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        UUID serviceUuid = getItem(position);
        holder.bind(serviceUuid);
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final TextView serviceUuidTextView;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceUuidTextView = itemView.findViewById(R.id.service_uuid_text_view);
        }

        public void bind(UUID serviceUuid) {
            serviceUuidTextView.setText(serviceUuid.toString());
        }
    }

    private static final DiffUtil.ItemCallback<UUID> DIFF_CALLBACK = new DiffUtil.ItemCallback<UUID>() {
        @Override
        public boolean areItemsTheSame(@NonNull UUID oldItem, @NonNull UUID newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull UUID oldItem, @NonNull UUID newItem) {
            return oldItem.equals(newItem);
        }
    };
    // TODO: Consider creating a dedicated data model class for BLE services instead of using raw UUID.
}
