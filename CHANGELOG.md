# Changelog

## 1
- Initial setup of the Android project.
- Added Nordic Semiconductor Android-BLE-Library and Android-Scanner-Compat-Library dependencies.
- Configured necessary Bluetooth permissions in AndroidManifest.xml.
- Implemented BLE scanning and display of devices in MainActivity using a RecyclerView.

## 2
- Commented out `BridgerManager.java` and `ScannerViewModel.java`.
- Replaced `MainActivity.java` with a simple "Hello World" activity.

## 3
- Created `PermissionsManager.java` to handle runtime permissions with a `CompletableFuture`-based API.
- Integrated `PermissionsManager` into `MainActivity.java` to request permissions on app start.
- Simplified `PermissionsManager.java` by removing `WeakReference` and making the `requestPermissions` method more concise.
- Simplified `MainActivity.java` by making `Log` messages more concise.
- Removed `Build.VERSION.SDK_INT` checks in `PermissionsManager.java` as `SDK_INT` is assumed to be >= 35.
- Removed unused `permissions` parameter from `PermissionsManager.onRequestPermissionsResult` and updated `MainActivity.java` accordingly.

## 4
- Added RxJava3 and RxAndroid3 dependencies to `app/build.gradle`.

## 5
- Removed redundant `setContentView(deviceListView)` call in `MainActivity.java` to resolve `IllegalStateException: The specified child already has a parent`.

## 6
- Updated `gradle/libs.versions.toml`:
    - `composeCompiler` updated to `1.5.11` for compatibility with Kotlin `1.9.23`.
- Updated `app/build.gradle`:
    - `compileSdk` changed from `36` to `34`.
    - `jvmTarget` in `kotlinOptions` changed from `'11'` to `'17'`.
- Updated `app/src/main/res/values/themes.xml` and `app/src/main/res/values-night/themes.xml`:
    - Changed parent theme from `Theme.MaterialComponents.DayNight.NoActionBar` to `Theme.Material3.DayNight.NoActionBar`.
    - Removed `colorPrimaryVariant` and `colorSecondaryVariant` attributes.
    - Updated `android:statusBarColor` to reference `?attr/colorPrimary`.

## 7
- Resolved Kotlin and Compose Compiler compatibility issues:
    - Updated `kotlin` version to `2.2.0` in `gradle/libs.versions.toml`.
    - Updated `composeCompiler` version to `2.2.0` in `gradle/libs.versions.toml`.
    - Defined `kotlin-compose-compiler` plugin in `gradle/libs.versions.toml`.
    - Replaced direct plugin `id 'org.jetbrains.kotlin.plugin.compose'` with `alias(libs.plugins.kotlin.compose.compiler)` in `app/build.gradle`.
    - Removed the `composeOptions` block from `app/build.gradle` as it's now managed by the plugin.
