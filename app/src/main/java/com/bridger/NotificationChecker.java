package com.bridger;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.bridger.events.SystemEvent; // Import SystemEvent
import com.bridger.services.NotificationService;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationChecker {

    private static final String TAG = "NotificationChecker";
    private static final int NOTIFICATION_ID = 1; // Our app's notification ID from NotificationService
    private final Context context;
    private final Store store;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public NotificationChecker(Context context) {
        this.context = context.getApplicationContext();
        this.store = Store.getInstance();
        setupEventSubscription();
    }

    private void setupEventSubscription() {
        disposables.add(store.getSystemEventSubject() // Subscribe to SystemEventSubject
                .filter(event -> event.getType() == SystemEvent.EventType.CHECK_NOTIFICATION_PRESENCE) // Use SystemEvent.EventType
                .subscribeOn(Schedulers.io())
                .subscribe(event -> checkAndRecreateNotification(),
                        throwable -> Log.e(TAG, "Error observing CHECK_NOTIFICATION_PRESENCE event: " + throwable.getMessage())));
    }

    private void checkAndRecreateNotification() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.w(TAG, "NotificationManager is null, cannot check notification status.");
            return;
        }

        try {
            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification sbn : activeNotifications) {
                if (sbn.getId() == NOTIFICATION_ID && sbn.getPackageName().equals(context.getPackageName())) return;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Cannot access active notifications. Ensure POST_NOTIFICATIONS permission is granted.", e);
        }


        Log.d(TAG, "Notification with ID " + NOTIFICATION_ID + " not active. Restarting NotificationService.");
        Intent serviceIntent = new Intent(context, NotificationService.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    public void dispose() {
        disposables.clear();
        Log.d(TAG, "NotificationChecker disposables cleared.");
    }
}
