# Changelog

## 1
- Initial setup of the Android project.
- Added Nordic Semiconductor Android-BLE-Library and Android-Scanner-Compat-Library dependencies.
- Configured necessary Bluetooth permissions in AndroidManifest.xml.
- Implemented BLE scanning and display of devices in MainActivity using a RecyclerView.

## 2
- Added RxJava and RxAndroid dependencies to `app/build.gradle`.
- Refactored `MainActivity.java`, `PermissionsManager.java`, and `BleScannerManager.java` to use RxJava for asynchronous operations, making the code more reactive and "JS-like".

## 3
- Implemented a robust, reactive BLE device scanning and display feature using RxJava.
- **Architecture & UI:** Replaced the initial `RecyclerView` implementation with a modern `ListAdapter` and `DiffUtil` for efficient UI updates. Introduced a `ScannerViewModel` to handle all business logic, separating concerns from the `MainActivity`.
- **Core Features:**
    - **Device Lifecycle:** Devices are now automatically removed from the list after a 30-second timeout if they are no longer visible.
    - **UI/UX:** The device list is sorted by signal strength (RSSI) in descending order. UI updates are throttled to prevent the list from "jumping" due to rapid RSSI changes, ensuring a smooth user experience.
- **Technical Implementation:**
    - The device list state is managed by a sophisticated RxJava stream using `groupBy`, `publish`, `throttleFirst`, and `debounce` operators to handle the lifecycle of each device individually and efficiently.
    - The state reducer was optimized to use a `Map` for O(1) performance on device updates and removals.
    - The entire codebase was refactored for readability and maintainability, with complex logic extracted into private methods.

## 4
- Implemented navigation to a new screen (`DeviceActivity`) upon clicking a device in the scanner list.
- Created `app/src/main/java/com/bridger/ui/device/` directory for device-specific UI components.
- Added `DeviceActivity.java`, `DeviceViewModel.java`, and `ServiceListAdapter.java` for displaying BLE services.
- Created `activity_device.xml` and `list_item_service.xml` layouts.
- Registered `DeviceActivity` in `AndroidManifest.xml`.
