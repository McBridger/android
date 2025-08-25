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
import android.content.pm.ServiceInfo; // Import ServiceInfo

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bridger.R;
import com.bridger.Store;
import com.bridger.model.ConnectionState; // Correct import for ConnectionState

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.core.Observable; // Import Observable
import com.bridger.model.NotificationContent; // Import NotificationContent

public class NotificationService extends Service {

    private static final String TAG = "NotificationService";
    public static final String CHANNEL_ID = "BridgerSyncChannel";
    public static final String ACTION_STOP_SERVICE = "com.bridger.ACTION_STOP_SERVICE";

    // Unique request codes for PendingIntents
    private static final int SYNC_PENDING_INTENT_REQUEST_CODE = 100;
    private static final int STOP_PENDING_INTENT_REQUEST_CODE = 101;
    private static final int DISMISSED_PENDING_INTENT_REQUEST_CODE = 102;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private Store store;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService onCreate: Service is being created.");
        store = Store.getInstance(); // Get the Store instance

        createNotificationChannel(); // Channel creation remains in onCreate as it's idempotent

        // Combine connection state and last action updates into a single stream
        disposables.add(Observable.combineLatest(
                        store.getConnectionStateSubject(),
                        store.getLastActionSubject(),
                        NotificationContent::from) // Use the static from method
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNotification,
                        throwable -> Log.e(TAG, "Error observing notification content: " + throwable.getMessage())));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "NotificationService onStartCommand: Service command received. Intent action: " + (intent != null ? intent.getAction() : "null"));

        // Get current state from Store to update notification immediately
        ConnectionState state = store.getConnectionStateSubject().getValue();
        String lastAction = store.getLastActionSubject().getValue();
        Notification notification = buildNotification(NotificationContent.from(state, lastAction));

        // Ensure notification is shown/re-shown every time onStartCommand is called
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notification);
        }
        Log.d(TAG, "NotificationService onStartCommand: startForeground called with current state.");

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (ACTION_STOP_SERVICE.equals(action)) {
                    Log.d(TAG, "NotificationService onStartCommand: ACTION_STOP_SERVICE received. Calling stopSelf().");
                    stopSelf();
                }
            }
        }
        Log.d(TAG, "NotificationService onStartCommand: Returning START_STICKY.");
        return START_STICKY; // Service will be restarted if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationService onDestroy: Service is being destroyed.");
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
                    NotificationManager.IMPORTANCE_HIGH // High importance for persistent notification
            );
            serviceChannel.setDescription("Manages persistent notification for clipboard synchronization.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(NotificationContent content) {
        // Action for "Tap to Sync" - directly launches ClipboardHandlerActivity
        Intent syncIntent = new Intent(this, com.bridger.ClipboardHandlerActivity.class);
        syncIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required for starting activity from notification
        PendingIntent syncPendingIntent = PendingIntent.getActivity(this,
                SYNC_PENDING_INTENT_REQUEST_CODE, syncIntent, PendingIntent.FLAG_IMMUTABLE);

        // Action for "Off" - stops the service
        Intent stopSelfIntent = new Intent(this, NotificationService.class);
        stopSelfIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopSelfPendingIntent = PendingIntent.getService(this,
                STOP_PENDING_INTENT_REQUEST_CODE, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create the intent that will be fired when the notification is dismissed
        Intent dismissedIntent = new Intent(this, NotificationDismissedReceiver.class);
        dismissedIntent.setAction(NotificationDismissedReceiver.ACTION_NOTIFICATION_DISMISSED); // Set the custom action
        PendingIntent dismissedPendingIntent = PendingIntent.getBroadcast(this,
                DISMISSED_PENDING_INTENT_REQUEST_CODE, dismissedIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(content.title())
                .setContentText(content.content())
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
                .setContentIntent(syncPendingIntent) // Main tap now triggers sync
                .addAction(R.drawable.ic_launcher_foreground, "Off", stopSelfPendingIntent) // Use a relevant icon
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE) // Ensure immediate and persistent display
                .setDeleteIntent(dismissedPendingIntent) // Attach the delete intent here
                .build();
    }

    private void updateNotification(NotificationContent content) {
        Notification notification = buildNotification(content);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification);
        }
    }
}
