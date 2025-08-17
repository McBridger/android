package com.bridger;

import android.content.ClipData;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ClipboardUtility {

    private static final String TAG = "ClipboardUtility";
    private static volatile ClipboardUtility instance;

    private final Context applicationContext;

    private ClipboardUtility(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    public static ClipboardUtility getInstance(Context context) {
        if (instance == null) {
            synchronized (ClipboardUtility.class) {
                if (instance == null) {
                    instance = new ClipboardUtility(context);
                }
            }
        }
        return instance;
    }

    /**
     * Reads the current text content from the system clipboard.
     *
     * @return The clipboard content as a String, or null if empty or non-text.
     */
    @Nullable
    public String readFromClipboard() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence text = clipData.getItemAt(0).coerceToText(applicationContext);
                if (text != null) {
                    Log.d(TAG, "Read from clipboard: " + text.toString());
                    return text.toString();
                }
            }
        }
        Log.d(TAG, "Clipboard is empty or contains non-text data.");
        return null;
    }

    /**
     * Writes the given text content to the system clipboard.
     *
     * @param text The text to write to the clipboard.
     */
    public void writeToClipboard(@NonNull String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Bridger Clipboard", text);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Updated system clipboard with: " + text);
        } else {
            Log.e(TAG, "Failed to get system clipboard service.");
        }
    }
}
