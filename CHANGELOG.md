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

## 6
- **Core Architecture:** Implemented `BleConnectionManager` as a Singleton to serve as a single source of truth for the BLE connection state, using the modern `ConnectionObserver` from the Nordic BLE library.
- **Constants:** Created `Constants.java` to centralize app-wide constants like service and characteristic UUIDs.
- **Reactive API:** The `BleConnectionManager` now exposes connection state, discovered services, and errors via a clean, observable RxJava API.

## 7
- **Phase 1: Scanner & Device Explorer Features and Bug Fixes:**
    - **Scanner UI Enhancement:** Implemented visual highlighting for devices advertising the `Bridger Sync Service` UUID in the scanner list. The `DeviceScanResult` class now encapsulates the logic for identifying a Bridger device, ensuring a clean separation of concerns, and `DeviceListViewHolder` uses this flag to apply a `teal_200` background color.
    - **Device Explorer UI:** Added "Connect", "Disconnect", and a hidden "Go to Sync Panel" button to `activity_device.xml`.
    - **Device Explorer Logic:** Implemented robust connection and disconnection functionality in `DeviceViewModel` using `BleConnectionManager`. `DeviceViewModel` now correctly observes and exposes discovered BLE services, which are displayed in `DeviceActivity` using `ServiceListAdapter`. The "Go to Sync Panel" button is conditionally visible based on the discovery of the `Bridger Sync Service`.
    - **View Binding:** Migrated `DeviceActivity` to use View Binding for cleaner and safer UI element access, and ensured `activity_device.xml` correctly includes all necessary UI elements for View Binding to resolve symbols.
    - **Navigation Fixes:** Corrected navigation from `MainActivity` to `DeviceActivity` to pass the `ScanResult` object as an `EXTRA_SCAN_RESULT`.
    - **API Alignment:** Corrected `DeviceViewModel` and `DeviceActivity`'s interactions with `BleConnectionManager`'s API, including using the correct `ConnectionState` enum, method names (`getConnectionState()`, `getDiscoveredServices()`), and argument types (`BluetoothDevice` for `connect()`, no arguments for `disconnect()`).
    - **Cleanup:** Removed the prematurely created `activity_connection.xml` file.

## 8
- **Sync Panel Implementation:**
    - Created and configured the core components for the sync panel: `ConnectionActivity`, `ConnectionViewModel`, and `activity_connection.xml`.
    - Implemented the UI, including a clipboard history `RecyclerView`, and connected it to the `ConnectionViewModel` to display connection status, errors, and sync history.
    - Integrated logic to handle large data transfers using the Nordic BLE library's `split()` and `merge()` operators.
    - Added a master `Switch` to `activity_main.xml` for global sync control.

## 9
- **Foreground Service & Event-Driven Architecture:**
    - Created `ClipboardSyncService` to manage the clipboard sync process in the background, complete with a persistent notification for user control.
    - Implemented "Tap to Sync" and "Off" actions in the notification.
    - Re-architected the communication between components by introducing a central, RxJava-based event bus (`PublishSubject<ClipboardEvent>`). This decoupled the `ConnectionViewModel` and `ClipboardSyncService` from each other and from the `BleConnectionManager`, replacing an initial `LocalBroadcastManager` implementation with a more robust, reactive pattern.

## 10
- **Core BLE Refactoring & Bug Fixes:**
    - Performed a major refactoring of `BleConnectionManager` to align with modern Nordic BLE library practices, resolving numerous deprecation warnings and compilation errors.
    - Re-architected internal characteristic operations for improved reliability and efficiency, ensuring characteristics are discovered once and reused.
    - Centralized notification setup within the connection initialization process.
    - Corrected various bugs related to data serialization, logging, and component initialization.

## 11
- **Consolidated Stability & Feature Enhancements**:
    - **Clipboard Access Refinement**: `ClipboardHandlerActivity` was significantly improved to provide robust and transparent clipboard access. Initially designed as an interactive dialog, it was refactored to be a fully transparent activity (`@android:style/Theme.Translucent.NoTitleBar`) that automatically reads clipboard content upon gaining focus (`onWindowFocusChanged`) and immediately finishes, minimizing visual interruption. Detailed logging was added for debugging, and its associated layout file (`activity_clipboard_handler.xml`) was removed. A duplicate entry for this activity in `AndroidManifest.xml` was also corrected.
    - **Permission & Notification Management**: All required foreground service permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS`) were added to `AndroidManifest.xml`, and `PermissionsManager.java` was updated for runtime handling. The "non-clickable notification" issue in `ClipboardSyncService` was resolved.

## 12
- **Reactive Architecture Implementation & Verification**:
    - Verified `BleConnectionManager` correctly dispatches `RECEIVED` events.
    - Verified `ClipboardUtility` is a pure utility.
    - Modified `ClipboardEvent.java` to add `createSendRequestedEvent` for data-carrying `SEND_REQUESTED` events.
    - Modified `ClipboardHandlerActivity.java` to dispatch `ClipboardEvent.createSendRequestedEvent` with clipboard data.
    - Verified `ConnectionViewModel` subscribes to `RECEIVED` events and writes to clipboard.
    - Refactored `ConnectionViewModel` to use separate, filtered RxJava subscriptions for `RECEIVED` and `SENT` events for cleaner code.
    - Initialized `BleConnectionManager` in `MainActivity.java` to ensure it's active from app startup.
- **Notification Persistence & Correctness Fixes**:
    - Updated `AndroidManifest.xml` with `FOREGROUND_SERVICE_DATA_SYNC` permission and set `foregroundServiceType="dataSync"` for `NotificationService`.
    - Modified `NotificationService.java` to use `startForeground` with `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` for modern Android.
    - Corrected `NotificationService.java`'s `contentIntent` to directly launch `ClipboardHandlerActivity` from the main notification tap.
    - Implemented "pseudo-persistent" notification using `deleteIntent`:
        - Created `NotificationDismissedReceiver.java` to re-launch `NotificationService` after a delay upon notification dismissal.
        - Registered `NotificationDismissedReceiver` in `AndroidManifest.xml` with a custom `intent-filter`.
        - Modified `NotificationService.java` to attach the `deleteIntent` to the notification builder, using a unique request code and `FLAG_UPDATE_CURRENT`.
    - Moved `startForeground` call from `onCreate` to `onStartCommand` in `NotificationService.java` to ensure notification re-display on service restart.
    - Ensured notification title reflects current connection state (e.g., "Connected", "Disconnected") with proper formatting (not all caps) when re-appearing.
- **Notification Service Refactoring**:
    - Consolidated RxJava streams for connection state and last action into a single `Observable.combineLatest` for unified notification updates.
    - Removed redundant "Tap to Sync" action button from the notification.
    - Assigned unique, constant-based request codes to `PendingIntent`s for improved safety and clarity.
- **Code Structure Improvement**:
    - Moved `NotificationContent` helper class from `NotificationService.java` to its own file (`NotificationContent.java`) in the `model` package for better organization and separation of concerns.
- **Notification `lastAction` Fix**:
    - Modified `BleConnectionManager.java` to update the `Store`'s `lastAction` with the actual clipboard content after a successful BLE write operation, ensuring the notification displays relevant information.

## 13
- **Notification Dismissal Fix**: Removed `.setOngoing(true)` from `NotificationService.java` to allow notifications to be dismissible, ensuring the `NotificationDismissedReceiver` is triggered and the notification is recreated as intended.

## 14
- **Notification Presence Check Optimization**: Optimized the notification presence check to use `NotificationManager.getActiveNotifications()` to verify if the persistent notification (ID 1) is truly missing before restarting the `NotificationService`. This prevents unnecessary service restarts and improves efficiency. The logic is handled by `NotificationChecker.java`, which subscribes to `SystemEvent.CHECK_NOTIFICATION_PRESENCE` dispatched from activities.
