package com.bridger;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.bridger.events.ClipboardEvent;

public class ClipboardHandlerActivity extends Activity {

    private static final String TAG = "ClipboardHandlerActivity";
    private boolean isClipboardProcessed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ClipboardHandlerActivity created.");
        // Do not read clipboard here as the activity does not have focus yet.
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && !isClipboardProcessed) {
            isClipboardProcessed = true;
            Log.d(TAG, "Activity has focus. Attempting to read and send clipboard.");
            sendCurrentClipboard();
            Log.d(TAG, "ClipboardHandlerActivity finished clipboard operation. Closing activity.");
            finish(); // Finish the activity after processing.
        }
    }

    private void sendCurrentClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (text != null) {
                String clipboardText = text.toString();
                BleConnectionManager.getInstance(getApplicationContext()).clipboardEvents.onNext(new ClipboardEvent.Send(clipboardText));
                Log.d(TAG, "Clipboard text sent: " + clipboardText);
            } else {
                Log.w(TAG, "Clipboard is empty or contains non-text data.");
            }
        } else {
            Log.w(TAG, "Clipboard is null or has no primary clip.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Finish the activity if it loses focus before the clipboard is processed.
        if (!isFinishing() && !isClipboardProcessed) { // Only finish if not already finishing and not processed
            Log.d(TAG, "ClipboardHandlerActivity paused before processing. Finishing.");
            finish();
        }
    }
}
