package com.bridger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class NotificationDismissedReceiver extends BroadcastReceiver {

    public static final String ACTION_NOTIFICATION_DISMISSED = "com.bridger.ACTION_NOTIFICATION_DISMISSED";
    private static final int RECREATE_DELAY_MS = 5000; // 5 seconds

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationDismissedReceiver", "onReceive: Notification dismissed broadcast received. Action: " + intent.getAction());
        // We use a Handler to delay the restart of the service.
        // This is crucial for a good user experience.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("NotificationDismissedReceiver", "onReceive: Attempting to restart NotificationService after delay.");
            Intent serviceIntent = new Intent(context, NotificationService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }, RECREATE_DELAY_MS);
    }
}
