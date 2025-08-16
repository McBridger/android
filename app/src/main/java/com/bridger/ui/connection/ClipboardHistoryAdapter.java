package com.bridger.ui.connection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bridger.R;

import java.util.Objects;

public class ClipboardHistoryAdapter extends ListAdapter<String, ClipboardHistoryAdapter.HistoryViewHolder> {

    public ClipboardHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_clipboard_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        String text = getItem(position);
        holder.bind(text);
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView historyTextView;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            historyTextView = itemView.findViewById(R.id.historyTextView);
        }

        public void bind(String text) {
            historyTextView.setText(text);
        }
    }

    private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return Objects.equals(oldItem, newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };
}
