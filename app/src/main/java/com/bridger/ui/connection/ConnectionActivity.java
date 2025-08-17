package com.bridger.ui.connection;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bridger.databinding.ActivityConnectionBinding;
import com.bridger.Store; // Import the Store
import com.bridger.events.ClipboardEvent; // Import ClipboardEvent
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import com.bridger.ui.scanner.DeviceListAdapter; // Import DeviceListAdapter for EXTRA_DEVICE_ADDRESS
import android.util.Log; // Import Log

public class ConnectionActivity extends AppCompatActivity {

    private ActivityConnectionBinding binding;
    private ConnectionViewModel viewModel;
    private ClipboardHistoryAdapter historyAdapter;
    private Store store; // Reference to the Store

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        store = Store.getInstance(); // Get the Store instance

        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(ConnectionViewModel.class);

        setupUI();
        observeViewModel();

        // Get device address from Intent and initiate connection via Store
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(DeviceListAdapter.EXTRA_DEVICE_ADDRESS)) {
            String deviceAddress = intent.getStringExtra(DeviceListAdapter.EXTRA_DEVICE_ADDRESS);
            if (deviceAddress != null) {
                Log.d("ConnectionActivity", "Attempting to connect to device: " + deviceAddress);
                store.dispatchClipboardEvent(ClipboardEvent.createConnectEvent(deviceAddress));
            } else {
                Log.e("ConnectionActivity", "Device address is null.");
                store.updateLastAction("Error: Device address missing.");
            }
        } else {
            Log.w("ConnectionActivity", "No device address passed via Intent. Assuming manual connection or persistence.");
            // In a future phase, we'll handle auto-reconnect/persistence here.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No need to stop ClipboardSyncService here, as it's replaced by NotificationService
        // and ClipboardUtility, which manage their own lifecycles or are singletons.
    }

    private void setupUI() {
        binding.shutdownSyncButton.setOnClickListener(v -> {
            // Dispatch a disconnect request to the Store
            store.dispatchClipboardEvent(ClipboardEvent.DISCONNECT_REQUESTED);
            Log.d("ConnectionActivity", "Disconnect requested via Store.");
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
                case FAILED: // Handle FAILED state explicitly
                    statusText = "Connection Status: Failed!";
                    break;
                default:
                    statusText = "Connection Status: Initializing...";
                    break;
            }
            binding.statusTextView.setText(statusText);
        });

        // Observe last action from ViewModel (which gets it from Store)
        viewModel.getLastAction().observe(this, lastAction -> {
            if (lastAction != null && !lastAction.isEmpty()) {
                // Update a separate text view or combine with status if desired
                // For now, we'll just log and let the history adapter show it.
                Log.d("ConnectionActivity", "Last action: " + lastAction);
            }
        });

        viewModel.getClipboardHistory().observe(this, history -> {
            historyAdapter.submitList(history);
            if (!history.isEmpty()) {
                binding.clipboardHistoryRecyclerView.scrollToPosition(0);
            }
        });
    }
}
