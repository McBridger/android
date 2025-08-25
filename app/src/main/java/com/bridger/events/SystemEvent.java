package com.bridger.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class SystemEvent {

    public enum EventType {
        CHECK_NOTIFICATION_PRESENCE // Request to check if the notification is present
    }

    private final EventType type;
    @Nullable private final String data; // Optional data associated with the event

    private SystemEvent(@NonNull EventType type, @Nullable String data) {
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
    public static SystemEvent CHECK_NOTIFICATION_PRESENCE = new SystemEvent(EventType.CHECK_NOTIFICATION_PRESENCE, null) {};

    @NonNull
    @Override
    public String toString() {
        return "SystemEvent: " + type + (data != null ? " (" + data + ")" : "");
    }
}
