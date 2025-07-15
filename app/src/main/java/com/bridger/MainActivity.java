package com.bridger;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";

  private ListView deviceListView;
  private ArrayAdapter<String> deviceListAdapter;
  private ArrayList<String> foundDevices;
  private HashSet<String> deviceAddresses;

  private PermissionsManager permissionsManager;
  private BleScannerManager bleScannerManager;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // 1. Загружаем наш XML-макет вместо создания View программно
    setContentView(R.layout.activity_main);

    // 2. Находим Toolbar по его ID и устанавливаем его как главную панель
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // 3. Находим наш ListView по его ID из XML-файла
    deviceListView = findViewById(R.id.device_list_view);

    foundDevices = new ArrayList<>();
    deviceAddresses = new HashSet<>();
    deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foundDevices);
    deviceListView.setAdapter(deviceListAdapter);
    permissionsManager = new PermissionsManager(this);
    bleScannerManager = new BleScannerManager();

    requestPermissionsAndStartScan();
  }

  private void requestPermissionsAndStartScan() {
    deviceListAdapter.add("Getting permissions...");

    disposables.add(
        permissionsManager.requestPermissions()
            .toObservable()
            // Шаг 1: Обрабатываем результат запроса разрешений
            .flatMap(this::handlePermissionResult) // <- Используем ссылку на метод
            // Шаг 2: Все последующие операции выполняем в UI потоке
            .observeOn(AndroidSchedulers.mainThread())
            // Шаг 3: Подготавливаем UI перед началом сканирования
            .doOnSubscribe(disposable -> prepareUiForScan())
            // Шаг 4: Подписываемся на результат, используя ссылки на методы
            .subscribe(
                this::onDeviceFound,    // <- Ссылка на метод для onSuccess
                this::onScanFailed      // <- Ссылка на метод для onError
            )
    );
  }

  //================================================================================
  // Приватные методы, на которые мы ссылаемся в Rx-цепочке
  //================================================================================

  /**
   * Метод для flatMap. Проверяет разрешения и решает, что делать дальше:
   * запустить сканирование или вернуть ошибку.
   * @param granted результат запроса разрешений.
   * @return Поток с результатами сканирования или поток с ошибкой.
   */
  private Observable<ScanResult> handlePermissionResult(boolean granted) {
    if (granted) {
      Log.d(TAG, "Permissions granted. Starting scan stream.");
      return bleScannerManager.getScanStream();
    } else {
      Log.e(TAG, "Permissions denied.");
      return Observable.error(new SecurityException("Permissions were denied."));
    }
  }

  /**
   * Метод для doOnSubscribe. Готовит UI к отображению результатов сканирования.
   */
  private void prepareUiForScan() {
    deviceListAdapter.clear();
    deviceListAdapter.add("Scanning for BLE devices...");
  }

  /**
   * Метод для onNext. Вызывается для каждого найденного устройства.
   * @param scanResult найденное устройство.
   */
  private void onDeviceFound(ScanResult scanResult) {
    String deviceAddress = scanResult.getDevice().getAddress();
    String deviceName = Objects.toString(
        scanResult.getScanRecord() != null ? scanResult.getScanRecord().getDeviceName() : null,
        "Unknown Device" // Значение по умолчанию, если имя null
    );

    String displayText = deviceName + "\n" + deviceAddress;

    if (foundDevices.isEmpty() || foundDevices.get(0).startsWith("Scanning")) {
      foundDevices.clear();
    }

    foundDevices.add(displayText);
    deviceListAdapter.notifyDataSetChanged();
    Log.d(TAG, "Found device: " + displayText);
  }

  /**
   * Метод для onError. Вызывается при любой ошибке в цепочке.
   * @param error произошедшая ошибка.
   */
  private void onScanFailed(Throwable error) {
    Log.e(TAG, "Chain failed: ", error);
    deviceListAdapter.clear();
    deviceListAdapter.add("Error: " + error.getMessage());
    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
  }

  //================================================================================
  // Остальные методы жизненного цикла
  //================================================================================

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (permissionsManager == null) return;

    permissionsManager.onResult(requestCode, grantResults);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    disposables.clear();
    Log.d(TAG, "MainActivity destroyed, all disposables cleared.");
  }
}
