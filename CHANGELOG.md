# Changelog

## 1
- Initial setup of the Android project.
- Added Nordic Semiconductor Android-BLE-Library and Android-Scanner-Compat-Library dependencies.
- Configured necessary Bluetooth permissions in AndroidManifest.xml.
- Implemented BLE scanning and display of devices in MainActivity using a RecyclerView.

## 2
- Added RxJava and RxAndroid dependencies to `app/build.gradle`.
- Refactored `MainActivity.java`, `PermissionsManager.java`, and `BleScannerManager.java` to use RxJava for asynchronous operations, making the code more reactive and "JS-like".
