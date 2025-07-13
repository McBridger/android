package com.bridger;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class MainActivity extends AppCompatActivity implements BluetoothDeviceAdapter.OnDeviceClickListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 2;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 3;
    private static final int REQUEST_ENABLE_BLUETOOTH = 4;
    private static final int REQUEST_ENABLE_LOCATION = 5;

    private ScannerViewModel scannerViewModel;
    private BluetoothDeviceAdapter deviceAdapter;
    private RecyclerView recyclerView;
    private BridgerManager bridgerManager;

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "Bluetooth enabled by user.");
                } else {
                    Log.w(TAG, "Bluetooth not enabled by user.");
                    Toast.makeText(this, "Bluetooth not enabled.", Toast.LENGTH_SHORT).show();
                }
                scannerViewModel.checkBluetoothAndLocationState(); // Re-check state after user action
            }
    );

    private final ActivityResultLauncher<Intent> enableLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Location settings activity returned.");
                scannerViewModel.checkBluetoothAndLocationState(); // Re-check state after user action
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        recyclerView = findViewById(R.id.recycler_view_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new BluetoothDeviceAdapter(new ArrayList<>());
        deviceAdapter.setOnDeviceClickListener(this); // Set the click listener
        recyclerView.setAdapter(deviceAdapter);

        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        bridgerManager = new BridgerManager(this);

        // Observe device scan results
        scannerViewModel.getDevices().observe(this, scanResults -> {
            Log.d(TAG, "Received " + scanResults.size() + " scan results.");
            deviceAdapter.clearDevices();
            for (ScanResult result : scanResults) {
                deviceAdapter.addDevice(result);
            }
            deviceAdapter.notifyDataSetChanged(); // Ensure UI updates
        });

        // Observe scanning state
        scannerViewModel.getScanning().observe(this, isScanning -> {
            Log.d(TAG, "Scanning state changed: " + isScanning);
            Toast.makeText(this, isScanning ? "Scanning..." : "Scan stopped.", Toast.LENGTH_SHORT).show();
        });

        // Observe Bluetooth enabled state
        scannerViewModel.getBluetoothEnabled().observe(this, isEnabled -> {
            Log.d(TAG, "Bluetooth enabled state: " + isEnabled);
            if (isEnabled != null && !isEnabled) {
                promptEnableBluetooth();
            } else {
                startScanIfReady(); // Try to start scan if Bluetooth is enabled
            }
        });

        // Observe Location enabled state
        scannerViewModel.getLocationEnabled().observe(this, isEnabled -> {
            Log.d(TAG, "Location enabled state: " + isEnabled);
            if (isEnabled != null && !isEnabled) {
                promptEnableLocation();
            } else {
                startScanIfReady(); // Try to start scan if Location is enabled
            }
        });

        // Initial permission check
        checkPermissionsAndStartScan();
        // Initial check of Bluetooth and Location state
        scannerViewModel.checkBluetoothAndLocationState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Checking Bluetooth and Location state.");
        scannerViewModel.checkBluetoothAndLocationState();
        // startScanIfReady() will be called by observers
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Stopping scan.");
        scannerViewModel.stopScan();
        bridgerManager.disconnect().enqueue(); // Disconnect from BLE device on pause
    }

    private void checkPermissionsAndStartScan() {
        Log.d(TAG, "Checking permissions.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting BLUETOOTH_SCAN permission.");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN_PERMISSION);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting BLUETOOTH_CONNECT permission.");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting ACCESS_FINE_LOCATION permission (API 31+).");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            } else {
                Log.d(TAG, "All required permissions granted for API 31+.");
                startScanIfReady();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting ACCESS_FINE_LOCATION permission (API 23-30).");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            } else {
                Log.d(TAG, "All required permissions granted for API 23-30.");
                startScanIfReady();
            }
        } else {
            Log.d(TAG, "No runtime permissions required for API < 23.");
            startScanIfReady();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted for request code: " + requestCode);
            checkPermissionsAndStartScan(); // Re-check all permissions
        } else {
            Log.w(TAG, "Permission denied for request code: " + requestCode);
            Toast.makeText(this, "Permissions denied. Cannot scan for devices.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startScanIfReady() {
        Log.d(TAG, "Attempting to start scan if ready.");
        Boolean bluetoothEnabled = scannerViewModel.getBluetoothEnabled().getValue();
        Boolean locationEnabled = scannerViewModel.getLocationEnabled().getValue();
        boolean allPermissionsGranted = hasAllRequiredPermissions();

        Log.d(TAG, "Bluetooth Enabled: " + bluetoothEnabled + ", Location Enabled: " + locationEnabled + ", All Permissions Granted: " + allPermissionsGranted);

        if (bluetoothEnabled != null && bluetoothEnabled &&
            locationEnabled != null && locationEnabled &&
            allPermissionsGranted) {
            Log.d(TAG, "Starting BLE scan.");
            scannerViewModel.startScan();
        } else {
            Log.d(TAG, "Cannot start scan. Conditions not met.");
        }
    }

    private boolean hasAllRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void promptEnableBluetooth() {
        Log.d(TAG, "Prompting user to enable Bluetooth.");
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBluetoothLauncher.launch(enableBtIntent);
    }

    private void promptEnableLocation() {
        Log.d(TAG, "Prompting user to enable Location services.");
        Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        enableLocationLauncher.launch(enableLocationIntent);
    }

    @Override
    public void onDeviceClick(ScanResult device) {
        Log.d(TAG, "Device clicked: " + device.getDevice().getName() + " (" + device.getDevice().getAddress() + ")");
        scannerViewModel.stopScan(); // Stop scanning when a device is clicked
        bridgerManager.connect(device.getDevice())
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue();

        // After successful connection, send "ping"
        bridgerManager.sendPing("ping");
    }
}
