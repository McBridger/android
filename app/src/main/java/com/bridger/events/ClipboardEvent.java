package com.bridger.events;

import androidx.annotation.NonNull;

public abstract class ClipboardEvent {

    private ClipboardEvent() {
        // Private constructor to prevent direct instantiation
    }

    public static final class Send extends ClipboardEvent {
        @NonNull public final String text;

        public Send(@NonNull String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return "Send: " + text;
        }
    }

    public static final class Receive extends ClipboardEvent {
        @NonNull public final String text;

        public Receive(@NonNull String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return "Receive: " + text;
        }
    }
}
