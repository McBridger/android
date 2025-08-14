package com.bridger.ui.device;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bridger.databinding.ActivityDeviceBinding;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

import static com.bridger.BleConnectionManager.ConnectionState; // Correct import for ConnectionState

public class DeviceActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_RESULT = "scan_result";

    private ActivityDeviceBinding binding;
    private DeviceViewModel viewModel;
    private ServiceListAdapter serviceListAdapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private ScanResult initialScanResult; // To hold the ScanResult passed from MainActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); // Correct way to set content view with View Binding

        // Retrieve the ScanResult object
        initialScanResult = getIntent().getParcelableExtra(EXTRA_SCAN_RESULT);

        if (initialScanResult == null) {
            Toast.makeText(this, "ScanResult not provided, cannot proceed.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);

        // Set device name and address
        String deviceName = initialScanResult.getScanRecord() != null ? initialScanResult.getScanRecord().getDeviceName() : "Unknown Device";
        binding.deviceNameTextView.setText(deviceName);
        binding.deviceAddressTextView.setText(initialScanResult.getDevice().getAddress());

        // Setup RecyclerView for services
        serviceListAdapter = new ServiceListAdapter();
        binding.servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.servicesRecyclerView.setAdapter(serviceListAdapter);

        // Setup buttons
        binding.connectButton.setOnClickListener(v -> viewModel.connect(initialScanResult));
        binding.disconnectButton.setOnClickListener(v -> viewModel.disconnect());
        binding.goToSyncButton.setOnClickListener(v -> {
            // TODO: Navigate to ConnectionActivity
            Toast.makeText(this, "Navigating to Sync Panel (Not yet implemented)", Toast.LENGTH_SHORT).show();
        });

        // Observe ViewModel streams
        disposables.add(viewModel.getConnectionStateStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateConnectionStateUi));

        disposables.add(viewModel.getDiscoveredServicesStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(serviceUuids -> serviceListAdapter.submitList(serviceUuids)));

        disposables.add(viewModel.getBridgerServiceFoundStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(found -> binding.goToSyncButton.setVisibility(found ? View.VISIBLE : View.GONE)));
    }

    private void updateConnectionStateUi(ConnectionState state) {
        String statusText;
        boolean isConnected = false;
        switch (state) { // Use the enum directly
            case CONNECTING:
                statusText = "Connecting...";
                break;
            case CONNECTED:
                statusText = "Connected";
                isConnected = true;
                break;
            case DISCONNECTING:
                statusText = "Disconnecting...";
                break;
            case DISCONNECTED:
                statusText = "Disconnected";
                break;
            // No STATE_FAILED or getReason() in our custom enum
            default:
                statusText = "Idle";
                break;
        }
        Toast.makeText(this, "Connection Status: " + statusText, Toast.LENGTH_SHORT).show();

        binding.connectButton.setEnabled(!isConnected);
        binding.disconnectButton.setEnabled(isConnected);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        // Ensure disconnect if activity is destroyed while connected
        if (isFinishing() && initialScanResult != null) {
            viewModel.disconnect();
        }
    }
}
