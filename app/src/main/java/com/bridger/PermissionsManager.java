package com.bridger;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;

public class PermissionsManager {

  private static final String TAG = "PermissionsManager";
  private static final int REQUEST_CODE_PERMISSIONS = 100;

  private final Activity activity;
  // Emitter is like (resolve, reject) => {} in a Promise. We save it to call later.
  private SingleEmitter<Boolean> permissionsEmitter;

  public PermissionsManager(Activity activity) {
    this.activity = activity;
  }

  // The method now returns Single<Boolean> - a direct analogue of Promise<boolean>
  public Single<Boolean> requestPermissions() {
    // Single.create is like new Promise(...)
    return Single.create(emitter -> {
      // Save the emitter to use it in onRequestPermissionsResult
      this.permissionsEmitter = emitter;

      List<String> permissionsToRequest = new ArrayList<>();
      // ... (all your permission checking logic remains the same)
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
      }
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
      }
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
      }

      if (permissionsToRequest.isEmpty()) {
        Log.d(TAG, "All required permissions already granted.");
        // If the emitter is still active, "resolve" the promise
        if (!emitter.isDisposed()) emitter.onSuccess(true);
      } else {
        Log.d(TAG, "Requesting permissions: " + Arrays.toString(permissionsToRequest.toArray()));
        ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest.toArray(new String[0]),
            REQUEST_CODE_PERMISSIONS
        );
      }
    });
  }

  public void onResult(int requestCode, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CODE_PERMISSIONS && permissionsEmitter != null && !permissionsEmitter.isDisposed()) {
      boolean allGranted = true;
      for (int result : grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
          allGranted = false;
          break;
        }
      }
      permissionsEmitter.onSuccess(allGranted); // <- "Resolve" the promise with the result
    }
  }
}
