# Bridger App Development Plan

## 1. High-Level Goal

Create a flexible, two-way clipboard synchronization service between an Android device and a MacBook using Bluetooth Low Energy (BLE), with robust tools for development and debugging.

## 2. Core Architecture: "Discover, Explore, Sync"

The application will follow a three-stage user flow, separating generic BLE functionality from the specific clipboard sync feature.

*   **Stage 1: Discover (`MainActivity`)** - Scan for all nearby BLE devices.
*   **Stage 2: Explore (`DeviceActivity`)** - Inspect a specific device's services and characteristics.
*   **Stage 3: Sync (`ConnectionActivity`)** - Manage an active, two-way clipboard sync session.

## 3. BLE Roles & Service Definition

*   **MacBook Role:** Server (Peripheral) - Advertises the `Bridger Sync Service`.
*   **Android Role:** Client (Central) - Scans for and connects to the MacBook.

### 3.1. The `Bridger Sync Service`
*   **Service UUID:** `A0B1C2D3-E4F5-46A7-B8C9-D0E1F2A3B4C5` (Example)
*   **Characteristics:**
    1.  `ANDROID_TO_MAC_CHARACTERISTIC` (UUID: `A1B2...`, Properties: `WRITE`)
    2.  `MAC_TO_ANDROID_CHARACTERISTIC` (UUID: `A2B3...`, Properties: `READ`, `NOTIFY`)

## 4. Android Application Implementation Plan

### Phase 1: The Scanner Screen (`MainActivity`)
1.  **Functionality:** Scan for and display all nearby BLE devices.
2.  **Highlighting:** Visually highlight any device in the list that is advertising the `Bridger Sync Service` UUID.
3.  **Navigation:** Tapping any device navigates to the `DeviceActivity`.

### Phase 2: The Device Explorer Screen (`DeviceActivity`)
1.  **Connection Control:** Feature a prominent "Connect" / "Disconnect" button.
2.  **Service/Characteristic Display:** Upon connection, list all of the device's services and their respective characteristics, including UUIDs and properties.
3.  **Conditional Navigation:** If the `Bridger Sync Service` is discovered, a "Go to Sync Panel" button will appear, which navigates to the `ConnectionActivity`.

### Phase 3: The Connection & Sync Screen (`ConnectionActivity`)
1.  **Purpose:** A dedicated screen to manage the active clipboard sync session.
2.  **UI Components:**
    *   Connection status indicator.
    *   "Shutdown Sync" button.
    *   A `RecyclerView` to log the history of exchanged text.
3.  **Sync Logic:**
    *   **Android -> Mac:** Triggered by a persistent notification tap. Reads the Android clipboard and writes it to the `ANDROID_TO_MAC_CHARACTERISTIC`.
    *   **Mac -> Android:** Listens for notifications on the `MAC_TO_ANDROID_CHARACTERISTIC` and updates the Android clipboard accordingly.
4.  **Notification:** The persistent "Tap to Sync" notification will be managed by this activity's lifecycle.

## 5. Implementation Details

### 5.1. Key Dependencies
The project relies on the following libraries from Nordic Semiconductor for robust and backward-compatible BLE functionality:
*   **BLE Communication:** `no.nordicsemi.android:ble:2.x` ([Android-BLE-Library](https://github.com/NordicSemiconductor/Android-BLE-Library))
*   **BLE Scanning:** `no.nordicsemi.android:scanner:1.x` ([Android-Scanner-Compat-Library](https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library))
*   **Reactive Programming:** RxJava (for asynchronous and event-driven programming, making the code more "JS-like")

## 6. TODOs

### Phase 0: Core Architecture & Setup
-   [x] **Constants:** Create a `Constants.java` file to hold all static app-wide constants, including the service and characteristic UUIDs.
-   [x] **Connection Manager:** Create a Singleton class `BleConnectionManager` to act as the single source of truth for the BLE connection state, including error states and disconnection events. All ViewModels and Services will observe this manager.

### Phase 1: Scanner & Explorer Enhancements
-   [x] **Scanner UI:** Modify `DeviceListViewHolder` to check if a `ScanResult` contains the `Bridger Sync Service` UUID and apply a visual highlight.
-   [x] **Device Explorer UI:** Add "Connect", "Disconnect", and a hidden "Go to Sync Panel" button to `activity_device.xml`.
-   [x] **Device Explorer Logic:** In `DeviceViewModel`, use the `BleConnectionManager` to implement the `connect()` and `disconnect()` methods.
-   [x] **Device Explorer Logic:** Upon connection, update the UI to list all discovered services and characteristics.
-   [x] **Device Explorer Logic:** If the `Bridger Sync Service` is found, make the "Go to Sync Panel" button visible.

### Phase 2: The Sync Panel (`ConnectionActivity`)
-   [x] **Create Files:** Create `ConnectionActivity.java`, `ConnectionViewModel.java`, and `activity_connection.xml`.
-   [x] **Register Activity:** Add `ConnectionActivity` to the `AndroidManifest.xml`.
-   [x] **Sync Panel UI:** Design `activity_connection.xml` with a status `TextView`, a "Shutdown Sync" button, and a `RecyclerView` for history.
-   [x] **Sync Panel UI:** The UI must react to connection and error states from the `BleConnectionManager` (e.g., show "Reconnecting...").
-   [x] **Sync Panel Logic:** Create a `ClipboardHistoryAdapter` for the `RecyclerView`.
-   [x] **Sync Panel Logic:** In `ConnectionViewModel`, implement the core sync logic:
    -   [x] Subscribe to notifications on the `MAC_TO_ANDROID_CHARACTERISTIC`. Use the Nordic library's `merge()` operator to handle large data.
    -   [x] On notification, update the Android clipboard and add the text to the history list. Ensure this UI update happens on the main thread (`AndroidSchedulers.mainThread()`).
    -   [x] Create a `sendClipboard(text)` method that writes to the `ANDROID_TO_MAC_CHARACTERISTIC`. Use the Nordic library's `split()` operator to handle large data.

### Phase 3: Foreground Service & Notification
-   [x] **Create Service:** Create a `ClipboardSyncService.java`.
-   [x] **Register Service:** Add `ClipboardSyncService` to the `AndroidManifest.xml`.
-   [x] **Service/Activity Communication:** Set up a `LocalBroadcastManager` for the notification to communicate with the `ConnectionActivity`.
-   [x] **Notification Logic:** Implement the logic to create and display the persistent notification.
    -   [x] The main tap action (`contentIntent`) should send a local broadcast to trigger the `sendClipboard` method.
    -   [x] The "Off" action button should stop the service.
-   [x] **Service Lifecycle:** The `ConnectionActivity` will be responsible for starting and stopping the `ClipboardSyncService`.

### Phase 4: Final UI Integration
-   [x] **Main Activity UI:** Add a `Switch` or `ToggleButton` to `activity_main.xml` for master on/off control (can be a later task).

### Phase 5: Stability & Seamlessness
-   [x] **Robust Transparent Activity for Clipboard Access:**
    -   [x] Implement a `ClipboardHandlerActivity` with a transparent theme (`@android:style/Theme.Translucent.NoTitleBar`).
    -   [x] The activity's sole purpose is to gain foreground access to read the clipboard.
    -   [x] In its `onCreate` or `onResume`, it will read the clipboard content and then immediately call `finish()` to close itself, minimizing any visual interruption.

### Phase 6: State Management & Service Refactoring
-   [x] **6.1: Implement Central State Store (`Store.java`)**
    -   [x] Create `Store.java` as a Singleton to act as the single source of truth.
    -   [x] Add `BehaviorSubject<ConnectionState>` for the global connection status.
    -   [x] Add `BehaviorSubject<String>` for the last synced action or text.
    -   [x] Add `PublishSubject<ClipboardEvent>` to handle all user and system-initiated events.
-   [x] **6.2: Create Notification Service & Logic Manager**
    -   [x] Create a new `services` directory: `app/src/main/java/com/bridger/services/`.
    -   [x] Create `NotificationService.java` as a dedicated foreground service. Its sole responsibility is to observe the `Store` and render the persistent notification, ensuring it always reflects the current app state.
    -   [x] Create `ClipboardManager.java` as a non-service Singleton. Its sole responsibility is to handle the business logic of clipboard synchronization by observing and reacting to events from the `Store`.
    -   [x] Remove the old `ClipboardSyncService.java` file and its registration in `AndroidManifest.xml`.
-   [x] **6.3: Refactor Core Components to Use Store**
    -   [x] Modify `MainActivity` to start the `NotificationService` on app launch, ensuring the notification is always present.
    -   [x] Refactor `BleConnectionManager` to push all state changes (connection, disconnection, errors) to the `Store` and subscribe to events (e.g., `SEND_CLIPBOARD`) from the `Store`.
    -   [x] Refactor all `ViewModels` (`ConnectionViewModel`, `DeviceViewModel`, etc.) to be stateless observers that source all their data directly from the `Store`.

### Phase 7: Streamline Navigation & Remove Redundancy
-   [x] **Update Core Architecture:** Change the app's core flow from "Discover, Explore, Sync" to a more direct "Discover, Sync" model.
-   [x] **Update Navigation Logic:**
    -   [x] In `MainActivity`'s `DeviceListAdapter`, modify the `onClick` handler to start `ConnectionActivity` directly.
    -   [x] Use an `Intent` extra to pass the selected device's address (a `String`) to `ConnectionActivity`. This is the standard Android practice for passing initial data.
-   [x] **Initiate Connection via Store:**
    -   [x] In `ConnectionActivity`'s `onCreate` method, retrieve the device address from the `Intent`.
    -   [x] Immediately dispatch a `connect` action to the `Store` with the retrieved address. From this point forward, the `Store` will manage the entire connection state.
-   [x] **Remove Redundant Components:** Delete the following files and remove any corresponding entries from `AndroidManifest.xml`:
    -   `DeviceActivity.java`
    -   `DeviceViewModel.java`
    -   `ServiceListAdapter.java`
    -   `activity_device.xml`
    -   `list_item_service.xml`
-   [x] **Ensure notification persistency**
    -   [x] **Move `NotificationContent` to `model` package:** Moved the `NotificationContent` helper class from `NotificationService.java` to `app/src/main/java/com/bridger/model/NotificationContent.java` for better code organization.
    -   [x] **Fix Notification `lastAction` Update:** Modified `BleConnectionManager.java` to update the `Store`'s `lastAction` with the actual clipboard content after a successful BLE write operation, ensuring the notification displays relevant information.

### Phase 8: Connection Stability (Leveraging the Store)
-   [ ] **Auto-Reconnect Logic:**
    -   [ ] Implement the auto-reconnect strategy (e.g., using `retryWhen`) within `BleConnectionManager`.
    -   [ ] Ensure that all intermediate states (`DISCONNECTED`, `CONNECTING`, etc.) during the reconnect attempts are published to the `Store`, so the entire app UI can react seamlessly.
-   [ ] **Connection Persistence:**
    -   [ ] On a successful connection event from the `Store`, save the device's MAC address to `SharedPreferences`.
    -   [ ] On app startup (`MainActivity`), check `SharedPreferences` for a saved address.
    -   [ ] If an address exists, dispatch a `connect` action to the `Store` immediately and navigate to `ConnectionActivity`, allowing the user to bypass the scanner and see the connection process in a unified way.

### Phase 9: UI/UX Overhaul (Component-Based Refactor)
-   [ ] **9.1: Create `HeaderView` Component:**
    -   [ ] Create `app/src/main/res/layout/view_header.xml` for the header's layout.
    -   [ ] Create `app/src/main/java/com/bridger/ui/HeaderView.java` as a custom view.
    -   [ ] Implement reactive logic in `HeaderView.java` to subscribe to `Store.getInstance().getConnectionStateSubject()` and dynamically change its background color (gray for idle/disconnected, deep blue for connecting/connected).
-   [ ] **9.2: Integrate `HeaderView`:**
    -   [ ] Replace the existing `Toolbar` in `app/src/main/res/layout/activity_main.xml` with the new `<com.bridger.ui.HeaderView />` component.
    -   [ ] Replace the existing `Toolbar` in `app/src/main/res/layout/activity_connection.xml` with the new `<com.bridger.ui.HeaderView />` component.
-   [ ] **9.3: Redesign `Device` Component:**
    -   [ ] Modify `app/src/main/res/layout/list_item_device_detailed.xml` to include elevation (shadows), an icon, and improved layout for better visual appeal.
-   [ ] **9.4: Redesign `ClipboardItem` Component:**
    -   [ ] Modify `app/src/main/res/layout/list_item_clipboard_history.xml` for a more visually distinct display, potentially with different styles for sent/received items.

### Phase 10: Dependency Injection with Hilt
-   [ ] **Setup Hilt:**
    -   [ ] Add Hilt dependencies to the `build.gradle` files.
    -   [ ] Create a custom `Application` class and annotate it with `@HiltAndroidApp`.
-   [ ] **Provide Dependencies:**
    -   [ ] Create a Hilt module (e.g., `AppModule`) to provide singleton instances of the `Store`, `BleConnectionManager`, and `ClipboardManager`.
-   [ ] **Inject Dependencies:**
    -   [ ] Refactor all Activities, Services, and ViewModels to receive their dependencies via `@Inject` in their constructors or fields, removing all manual singleton access (`.getInstance()`).

### Phase 11: Code Refactoring
-   [ ] **Refactor `BleConnectionManager` for Unified Characteristic Handling:**
    1.  [ ] **Unified Callback Model:** Modify the `Characteristic` model to hold separate functional interfaces for read and write operations.
    2.  [ ] **Declarative Map:** Update the `SUPPORTED_CHARACTERISTICS` map to use the new callback model.
    3.  [ ] **Generic `write` Method:** Create a single, generic public `writeToCharacteristic(UUID, Data)` method.

### Phase 12: Testing
-   [ ] **Unit Tests:** Add unit tests, leveraging Hilt for easy dependency mocking.
-   [ ] **Integration Tests:** Add integration tests for the clipboard sync flow.

### Phase 13: Future Modernization (Kotlin Migration)
-   [ ] **Progressive Kotlin Migration:**
    -   [ ] Enable Kotlin in the project.
    -   [ ] Begin migrating existing Java classes to Kotlin, starting with data/model classes and moving towards UI and business logic.
    -   [ ] Explore replacing RxJava with Kotlin Coroutines and Flow for asynchronous operations in new features.
