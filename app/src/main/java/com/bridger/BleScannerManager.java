package com.bridger;

import android.util.Log;
import androidx.annotation.NonNull;
import java.util.List;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class BleScannerManager {

  private static final String TAG = "BleScannerManager";
  private final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
  private final ScanSettings scanSettings;
  private final Observable<ScanResult> sharedScanStream; // Field for the hot stream

  public BleScannerManager() {
    this.scanSettings = new ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .setLegacy(false)
        .build();

    // Initialize the hot stream here
    this.sharedScanStream = createRawScanObservable()
        // .distinct(scanResult -> scanResult.getDevice().getAddress())
        .share(); // Convert to a hot Observable
  }

  /**
   * Returns a stream of raw scan results.
   * This stream is now "hot" and shared among all subscribers.
   */
  public Observable<ScanResult> getScanStream() {
    return this.sharedScanStream; // Return the already prepared hot stream
  }

  /**
   * Creates a "raw" Observable that emits all scan results.
   */
  private Observable<ScanResult> createRawScanObservable() {
    return Observable.create(emitter -> {
      // 1. Create a callback, passing it the emitter to communicate with the stream.
      //    Use final, as it is referenced from the unsubscribe lambda.
      final ScanCallback scanCallback = createScanCallbackForEmitter(emitter);

      Log.d(TAG, "Starting BLE scan...");
      // 2. Start scanning with this callback.
      scanner.startScan(null, this.scanSettings, scanCallback);

      // 3. When unsubscribing, stop scanning using the same callback.
      emitter.setCancellable(() -> {
        Log.d(TAG, "Stopping BLE scan...");
        scanner.stopScan(scanCallback);
      });
    });
  }

  /**
   * Factory method that creates and returns a new ScanCallback.
   * It acts as a bridge between the Android callback world and the Rx world.
   * @param emitter The emitter through which the callback will send data to the stream.
   * @return A fully configured ScanCallback.
   */
  private ScanCallback createScanCallbackForEmitter(ObservableEmitter<ScanResult> emitter) {
    return new ScanCallback() {
      @Override
      public void onScanResult(int callbackType, @NonNull ScanResult result) {
        super.onScanResult(callbackType, result);
        if (emitter.isDisposed()) return;;
        emitter.onNext(result);
      }

      @Override
      public void onBatchScanResults(@NonNull List<ScanResult> results) {
        super.onBatchScanResults(results);
        if (emitter.isDisposed()) return;;

        for (ScanResult result : results)
          emitter.onNext(result);
      }

      @Override
      public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        if (emitter.isDisposed()) return;

        emitter.onError(new Throwable("Scan failed with error code: " + errorCode));
      }
    };
  }
}
