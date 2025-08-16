package com.bridger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bridger.events.ClipboardEvent; // Import ClipboardEvent

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ClipboardSyncService extends Service {

    private static final String TAG = "ClipboardSyncService";
    public static final String ACTION_SEND_CLIPBOARD = "com.bridger.ACTION_SEND_CLIPBOARD";
    public static final String ACTION_STOP_SERVICE = "com.bridger.ACTION_STOP_SERVICE";

    private static final String CHANNEL_ID = "BridgerSyncChannel";
    private static final int NOTIFICATION_ID = 1;

    private BleConnectionManager bleConnectionManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        bleConnectionManager = BleConnectionManager.getInstance(getApplicationContext());

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        disposables.add(bleConnectionManager.getConnectionState()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        connectionState -> {
                            if (connectionState == BleConnectionManager.ConnectionState.DISCONNECTED) {
                                Log.d(TAG, "BLE disconnected, stopping service.");
                                stopSelf();
                            }
                        },
                        throwable -> Log.e(TAG, "Error observing connection state in service", throwable)
                ));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        if (intent != null) {
            String action = intent.getAction();
            // The ACTION_SEND_CLIPBOARD is now handled by ClipboardHandlerActivity
            // This block is kept for potential future direct service actions, but currently unused for clipboard send.
            if (ACTION_STOP_SERVICE.equals(action)) {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        disposables.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bridger Sync Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        // This intent will now launch the transparent ClipboardHandlerActivity
        Intent sendClipboardIntent = new Intent(this, ClipboardHandlerActivity.class);
        PendingIntent sendClipboardPendingIntent = PendingIntent.getActivity(
                this,
                0,
                sendClipboardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopServiceIntent = new Intent(this, ClipboardSyncService.class);
        stopServiceIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopServicePendingIntent = PendingIntent.getService(
                this,
                1,
                stopServiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bridger Clipboard Sync")
                .setContentText("Tap to sync clipboard to Mac")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(sendClipboardPendingIntent) // Make the main notification body clickable
                .addAction(R.drawable.ic_launcher_foreground, "Tap to Sync", sendClipboardPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Off", stopServicePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
}
