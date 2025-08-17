package com.bridger;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.bridger.events.ClipboardEvent;

public class ClipboardHandlerActivity extends Activity {

    private static final String TAG = "ClipboardHandlerActivity";
    private boolean isClipboardProcessed = false;
    private ClipboardUtility clipboardUtility; // Reference to ClipboardUtility
    private Store store; // Reference to Store

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ClipboardHandlerActivity created.");
        clipboardUtility = ClipboardUtility.getInstance(getApplicationContext());
        store = Store.getInstance();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && !isClipboardProcessed) {
            isClipboardProcessed = true;
            Log.d(TAG, "Activity has focus. Attempting to read and dispatch clipboard.");
            readAndDispatchClipboard();
            Log.d(TAG, "ClipboardHandlerActivity finished clipboard operation. Closing activity.");
            finish(); // Finish the activity after processing.
        }
    }

    private void readAndDispatchClipboard() {
        String clipboardText = clipboardUtility.readFromClipboard();
        if (clipboardText != null) {
            store.dispatchClipboardEvent(ClipboardEvent.createSendRequestedEvent(clipboardText));
            Log.d(TAG, "Clipboard text read and dispatched to Store: " + clipboardText);
        } else {
            Log.w(TAG, "Clipboard is empty or contains non-text data. No event dispatched.");
            store.updateLastAction("Clipboard empty."); // Update last action in Store
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
