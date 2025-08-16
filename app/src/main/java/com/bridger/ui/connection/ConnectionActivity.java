package com.bridger.ui.connection;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bridger.databinding.ActivityConnectionBinding;
import com.bridger.BleConnectionManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import android.os.Build;
import com.bridger.ClipboardSyncService;

public class ConnectionActivity extends AppCompatActivity {

    private ActivityConnectionBinding binding;
    private ConnectionViewModel viewModel;
    private ClipboardHistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(ConnectionViewModel.class);

        setupUI();
        observeViewModel();

        // Start the ClipboardSyncService
        startClipboardSyncService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the ClipboardSyncService when activity is destroyed
        stopClipboardSyncService();
    }

    private void setupUI() {
        binding.shutdownSyncButton.setOnClickListener(v -> {
            BleConnectionManager.getInstance(getApplicationContext()).disconnect();
            stopClipboardSyncService();
        });

        historyAdapter = new ClipboardHistoryAdapter();
        binding.clipboardHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.clipboardHistoryRecyclerView.setAdapter(historyAdapter);
    }

    private void observeViewModel() {
        viewModel.getConnectionState().observe(this, connectionState -> {
            String statusText;
            switch (connectionState) {
                case CONNECTING:
                    statusText = "Connection Status: Connecting...";
                    break;
                case CONNECTED:
                    statusText = "Connection Status: Connected";
                    break;
                case DISCONNECTING:
                    statusText = "Connection Status: Disconnecting...";
                    break;
                case DISCONNECTED:
                    statusText = "Connection Status: Disconnected";
                    break;
                case READY:
                    statusText = "Connection Status: Ready for Sync!";
                    break;
                case INITIALIZING:
                default:
                    statusText = "Connection Status: Initializing...";
                    break;
            }
            binding.statusTextView.setText(statusText);
        });

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                binding.statusTextView.setText("Error: " + errorMessage);
            }
        });

        viewModel.getClipboardHistory().observe(this, history -> {
            historyAdapter.submitList(history);
            if (!history.isEmpty()) {
                binding.clipboardHistoryRecyclerView.scrollToPosition(0);
            }
        });
    }

    private void startClipboardSyncService() {
        Intent serviceIntent = new Intent(this, ClipboardSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopClipboardSyncService() {
        Intent serviceIntent = new Intent(this, ClipboardSyncService.class);
        stopService(serviceIntent);
    }
}
