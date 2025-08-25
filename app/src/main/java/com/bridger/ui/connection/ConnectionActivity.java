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
import com.bridger.NotificationChecker; // Import NotificationChecker
import com.bridger.events.SystemEvent; // Import SystemEvent

public class ConnectionActivity extends AppCompatActivity {

    private ActivityConnectionBinding binding;
    private ConnectionViewModel viewModel;
    private ClipboardHistoryAdapter historyAdapter;
    private Store store; // Reference to the Store
    private NotificationChecker notificationChecker; // Declare NotificationChecker

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        store = Store.getInstance(); // Get the Store instance

        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(ConnectionViewModel.class);

        notificationChecker = new NotificationChecker(getApplicationContext()); // Initialize NotificationChecker

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
    protected void onResume() {
        super.onResume();
        // Dispatch event to check notification presence
        Store.getInstance().dispatchSystemEvent(SystemEvent.CHECK_NOTIFICATION_PRESENCE);
        Log.d("ConnectionActivity", "onResume: Dispatched CHECK_NOTIFICATION_PRESENCE system event.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationChecker.dispose(); // Dispose of NotificationChecker
        Log.d("ConnectionActivity", "ConnectionActivity destroyed, notificationChecker disposed.");
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
