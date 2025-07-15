package com.bridger

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import no.nordicsemi.android.support.v18.scanner.ScanResult

class MainActivity : ComponentActivity() {

    // Менеджеры и подписки остаются на уровне Activity,
    // так как их жизненный цикл привязан к ней.
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var bleScannerManager: BleScannerManager
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем наши менеджеры
        permissionsManager = PermissionsManager(this)
        bleScannerManager = BleScannerManager()

        // setContent — это точка входа в Jetpack Compose.
        // Заменяет собой setContentView(R.layout.activity_main)
        setContent {
            // Передаем в наш Composable-экран все, что ему нужно для работы.
            BleScannerScreen(
                permissionsManager = permissionsManager,
                bleScannerManager = bleScannerManager,
                disposables = disposables
            )
        }
    }

    // Этот метод все еще нужен для получения результата запроса разрешений.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очищаем все RxJava подписки при уничтожении Activity, чтобы избежать утечек.
        disposables.clear()
        Log.d("MainActivity", "MainActivity destroyed, all disposables cleared.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScannerScreen(
    permissionsManager: PermissionsManager,
    bleScannerManager: BleScannerManager,
    disposables: CompositeDisposable
) {
    // --- Управление состоянием ---
    // Состояние для списка найденных устройств. `mutableStateListOf` заставляет
    // Compose перерисовать список при добавлении/удалении элементов.
    val foundDevices = remember { mutableStateListOf<String>() }
    // `mutableStateOf` для хранения одиночного значения (статусного сообщения).
    val statusMessage = remember { mutableStateOf("Requesting permissions...") }
    val context = LocalContext.current // Контекст для показа Toast-сообщений.

    // --- Сайд-эффекты ---
    // `LaunchedEffect` — это специальный Composable для запуска корутин или,
    // в нашем случае, для подписки на RxJava-поток.
    // `key1 = Unit` означает, что эффект запустится один раз, когда
    // Composable появится на экране.
    LaunchedEffect(key1 = Unit) {
        val scanStream = permissionsManager.requestPermissions()
            .toObservable()
            .flatMap { granted ->
                if (granted) {
                    Log.d("BleScannerScreen", "Permissions granted. Starting scan stream.")
                    bleScannerManager.scanStream
                } else {
                    Log.e("BleScannerScreen", "Permissions denied.")
                    Observable.error(SecurityException("Permissions were denied."))
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                // Подготовка UI к сканированию
                foundDevices.clear()
                statusMessage.value = "Scanning for BLE devices..."
            }

        // Подписываемся на поток и добавляем подписку в `disposables`
        disposables.add(
            scanStream.subscribe(
                { scanResult -> // onNext
                    val deviceAddress = scanResult.device.address
                    val deviceName = scanResult.scanRecord?.deviceName ?: "Unknown Device"
                    val displayText = "$deviceName\n$deviceAddress"

                    // Убираем статусное сообщение, как только появляется первое устройство
                    if (statusMessage.value.isNotEmpty()) {
                        statusMessage.value = ""
                    }
                    
                    // Добавляем устройство, только если его еще нет в списке, чтобы избежать дубликатов
                    if (!foundDevices.contains(displayText)) {
                        foundDevices.add(displayText)
                    }
                    Log.d("BleScannerScreen", "Found device: $displayText")
                },
                { error -> // onError
                    Log.e("BleScannerScreen", "Chain failed: ", error)
                    foundDevices.clear()
                    statusMessage.value = "Error: ${error.message}"
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        )
    }

    // --- Декларативное описание UI ---
    // Используем тему Material 3, которую мы настроили в XML.
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("BLE Scanner (Compose)") })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                // Если есть статусное сообщение, показываем его
                if (statusMessage.value.isNotEmpty()) {
                    Text(
                        text = statusMessage.value,
                        modifier = Modifier.padding(all = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // "Ленивый" список, который эффективно отображает только видимые элементы.
                // Это замена RecyclerView/ListView.
                LazyColumn {
                    items(foundDevices) { deviceText ->
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = deviceText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Divider() // Разделитель между элементами
                        }
                    }
                }
            }
        }
    }
}