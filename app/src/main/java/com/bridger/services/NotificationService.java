package com.bridger.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bridger.MainActivity;
import com.bridger.R;
import com.bridger.Store;
import com.bridger.events.ClipboardEvent;
import com.bridger.model.ConnectionState; // Correct import for ConnectionState

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationService extends Service {

    private static final String TAG = "NotificationService";
    public static final String CHANNEL_ID = "BridgerSyncChannel";
    public static final String ACTION_SYNC_CLIPBOARD = "com.bridger.ACTION_SYNC_CLIPBOARD";
    public static final String ACTION_STOP_SERVICE = "com.bridger.ACTION_STOP_SERVICE";

    private final CompositeDisposable disposables = new CompositeDisposable();
    private Store store;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService onCreate");
        store = Store.getInstance(); // Get the Store instance

        createNotificationChannel();
        startForeground(1, buildNotification("Initializing...", "Tap to sync clipboard"));

        // Subscribe to connection state changes from the Store
        disposables.add(store.getConnectionStateSubject()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNotificationBasedOnConnectionState,
                        throwable -> Log.e(TAG, "Error observing connection state: " + throwable.getMessage())));

        // Subscribe to last action changes from the Store
        disposables.add(store.getLastActionSubject()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNotificationLastAction,
                        throwable -> Log.e(TAG, "Error observing last action: " + throwable.getMessage())));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "NotificationService onStartCommand");

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_STOP_SERVICE:
                        Log.d(TAG, "ACTION_STOP_SERVICE received. Stopping service.");
                        stopSelf();
                        break;
                }
            }
        }
        return START_STICKY; // Service will be restarted if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationService onDestroy");
        disposables.clear(); // Clear all RxJava subscriptions
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't need to bind to this service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bridger Sync Service Channel",
                    NotificationManager.IMPORTANCE_LOW // Low importance to minimize interruption
            );
            serviceChannel.setDescription("Manages persistent notification for clipboard synchronization.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Action for "Tap to Sync"
        Intent syncIntent = new Intent(this, com.bridger.ClipboardHandlerActivity.class);
        syncIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required for starting activity from notification
        PendingIntent syncPendingIntent = PendingIntent.getActivity(this,
                0, syncIntent, PendingIntent.FLAG_IMMUTABLE);

        // Action for "Off"
        Intent stopSelfIntent = new Intent(this, NotificationService.class);
        stopSelfIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopSelfPendingIntent = PendingIntent.getService(this,
                0, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Tap to Sync", syncPendingIntent) // Use a relevant icon
                .addAction(R.drawable.ic_launcher_foreground, "Off", stopSelfPendingIntent) // Use a relevant icon
                .setOngoing(true) // Makes the notification non-dismissible
                .build();
    }

    private void updateNotification(String title, String content) {
        Notification notification = buildNotification(title, content);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification);
        }
    }

    private void updateNotificationBasedOnConnectionState(ConnectionState state) {
        String title;
        String content;
        switch (state) {
            case CONNECTED:
                title = "Bridger: Connected";
                content = "Ready to sync clipboard.";
                break;
            case CONNECTING:
                title = "Bridger: Connecting...";
                content = "Attempting to establish BLE connection.";
                break;
            case DISCONNECTED:
                title = "Bridger: Disconnected";
                content = "Tap to reconnect or scan.";
                break;
            case DISCONNECTING:
                title = "Bridger: Disconnecting...";
                content = "Closing BLE connection.";
                break;
            case FAILED:
                title = "Bridger: Connection Failed";
                content = "Tap to retry.";
                break;
            default:
                title = "Bridger: Unknown State";
                content = "Service running.";
                break;
        }
        updateNotification(title, content);
    }

    private void updateNotificationLastAction(String lastAction) {
        // This method can be used to update the notification content with the last action
        // For simplicity, we'll just update the content text of the existing notification.
        // In a more complex scenario, you might want to combine this with connection state.
        String currentTitle = "Bridger: " + store.getConnectionStateSubject().getValue().name(); // Get current state for title
        updateNotification(currentTitle, lastAction);
    }
}
