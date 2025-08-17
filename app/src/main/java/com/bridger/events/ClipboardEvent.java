package com.bridger.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Base abstract class for all clipboard-related events
public abstract class ClipboardEvent {

    public enum EventType {
        SEND_REQUESTED, // User tapped "Tap to Sync" or similar
        SENT,           // Clipboard content successfully sent via BLE
        RECEIVED,       // Clipboard content received via BLE
        CONNECT_REQUESTED, // User requested to connect to a device
        DISCONNECT_REQUESTED // User requested to disconnect
    }

    private final EventType type;
    @Nullable private final String data; // Optional data associated with the event (e.g., clipboard text, device address)

    private ClipboardEvent(@NonNull EventType type, @Nullable String data) {
        this.type = type;
        this.data = data;
    }

    @NonNull
    public EventType getType() {
        return type;
    }

    @Nullable
    public String getData() {
        return data;
    }

    // Factory methods for specific event types

    public static ClipboardEvent DISCONNECT_REQUESTED = new ClipboardEvent(EventType.DISCONNECT_REQUESTED, null) {};

    public static ClipboardEvent createSendRequestedEvent(@NonNull String text) {
        return new ClipboardEvent(EventType.SEND_REQUESTED, text) {};
    }

    public static ClipboardEvent createSentEvent(@NonNull String text) {
        return new ClipboardEvent(EventType.SENT, text) {};
    }

    public static ClipboardEvent createReceiveEvent(@NonNull String text) {
        return new ClipboardEvent(EventType.RECEIVED, text) {};
    }

    public static ClipboardEvent createConnectEvent(@NonNull String deviceAddress) {
        return new ClipboardEvent(EventType.CONNECT_REQUESTED, deviceAddress) {};
    }

    @NonNull
    @Override
    public String toString() {
        return "ClipboardEvent: " + type + (data != null ? " (" + data + ")" : "");
    }
}
