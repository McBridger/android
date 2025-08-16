package com.bridger;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button; // Import Button

import com.bridger.events.ClipboardEvent;

public class ClipboardHandlerActivity extends Activity {

    private static final String TAG = "ClipboardHandlerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the new layout for the dialog-themed activity
        setContentView(R.layout.activity_clipboard_handler);

        Button sendButton = findViewById(R.id.sendClipboardButton);
        sendButton.setOnClickListener(v -> {
            Log.d(TAG, "Send Clipboard button clicked. Attempting to send clipboard.");
            sendCurrentClipboard();
            finish(); // Finish the activity after sending
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // No automatic clipboard sending here. It will be triggered by button click.
    }

    private void sendCurrentClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (text != null) {
                BleConnectionManager.getInstance(getApplicationContext()).clipboardEvents.onNext(new ClipboardEvent.Send(text.toString()));
                Log.d(TAG, "Clipboard text sent via ClipboardHandlerActivity: " + text.toString());
            } else {
                Log.w(TAG, "Clipboard is empty or contains non-text data.");
            }
        } else {
            Log.w(TAG, "Clipboard is null or has no primary clip.");
        }
    }
}
