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
-   [ ] **Create Files:** Create `ConnectionActivity.java`, `ConnectionViewModel.java`, and `activity_connection.xml`.
-   [ ] **Register Activity:** Add `ConnectionActivity` to the `AndroidManifest.xml`.
-   [ ] **Sync Panel UI:** Design `activity_connection.xml` with a status `TextView`, a "Shutdown Sync" button, and a `RecyclerView` for history.
-   [ ] **Sync Panel UI:** The UI must react to connection and error states from the `BleConnectionManager` (e.g., show "Reconnecting...").
-   [ ] **Sync Panel Logic:** Create a `ClipboardHistoryAdapter` for the `RecyclerView`.
-   [ ] **Sync Panel Logic:** In `ConnectionViewModel`, implement the core sync logic:
    -   [ ] Subscribe to notifications on the `MAC_TO_ANDROID_CHARACTERISTIC`. Use the Nordic library's `merge()` operator to handle large data.
    -   [ ] On notification, update the Android clipboard and add the text to the history list. Ensure this UI update happens on the main thread (`AndroidSchedulers.mainThread()`).
    -   [ ] Create a `sendClipboard(text)` method that writes to the `ANDROID_TO_MAC_CHARACTERISTIC`. Use the Nordic library's `split()` operator to handle large data.

### Phase 3: Foreground Service & Notification
-   [ ] **Create Service:** Create a `ClipboardSyncService.java`.
-   [ ] **Register Service:** Add `ClipboardSyncService` to the `AndroidManifest.xml`.
-   [ ] **Service/Activity Communication:** Set up a `LocalBroadcastManager` for the notification to communicate with the `ConnectionActivity`.
-   [ ] **Notification Logic:** Implement the logic to create and display the persistent notification.
    -   [ ] The main tap action (`contentIntent`) should send a local broadcast to trigger the `sendClipboard` method.
    -   [ ] The "Off" action button should stop the service.
-   [ ] **Service Lifecycle:** The `ConnectionActivity` will be responsible for starting and stopping the `ClipboardSyncService`.

### Phase 4: Final UI Integration
-   [ ] **Main Activity UI:** Add a `Switch` or `ToggleButton` to `activity_main.xml` for master on/off control (can be a later task).
