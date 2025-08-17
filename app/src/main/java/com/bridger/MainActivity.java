package com.bridger;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import com.bridger.databinding.ActivityMainBinding;
import com.bridger.ui.scanner.DeviceListAdapter;
import com.bridger.ui.scanner.ScannerViewModel;
import com.bridger.services.NotificationService; // Import NotificationService
import android.content.Intent; // Import Intent
import androidx.core.content.ContextCompat; // Import ContextCompat

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";

  private DeviceListAdapter deviceListAdapter;
  private ScannerViewModel scannerViewModel;
  private PermissionsManager permissionsManager;

  private final CompositeDisposable activityDisposables = new CompositeDisposable();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Initialize BleConnectionManager
    BleConnectionManager.getInstance(getApplicationContext());

    // Start the NotificationService
    Intent serviceIntent = new Intent(this, NotificationService.class);
    ContextCompat.startForegroundService(this, serviceIntent);

    deviceListAdapter = new DeviceListAdapter(this); // Pass context here
    binding.deviceListView.setLayoutManager(new LinearLayoutManager(this));
    binding.deviceListView.setAdapter(deviceListAdapter);

    scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
    permissionsManager = new PermissionsManager(this);

    activityDisposables.add(
        permissionsManager.requestPermissions()
            .toObservable()
            .flatMap(granted -> {
                if (granted) {
                    Log.d(TAG, "Permissions granted. Starting scan.");
                    scannerViewModel.startScan();
                    return scannerViewModel.getDevicesStream();
                } else {
                    Log.e(TAG, "Permissions denied. Cannot start scan.");
                    Toast.makeText(this, "Permissions denied. Cannot scan for devices.", Toast.LENGTH_LONG).show();
                    return Observable.error(new SecurityException("Permissions were denied."));
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                deviceListAdapter::submitList,
                error -> {
                    Log.e(TAG, "Scan or permission chain failed: ", error);
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            )
    );
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    permissionsManager.onResult(requestCode, grantResults);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    activityDisposables.clear();
    Log.d(TAG, "MainActivity destroyed, activity disposables cleared.");
  }
}
