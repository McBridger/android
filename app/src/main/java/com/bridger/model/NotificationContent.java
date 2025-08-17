package com.bridger.model;

public record NotificationContent(String title, String content) {

    public static NotificationContent from(ConnectionState state, String lastAction) {
        String formattedTitle = "Bridger: " + formatConnectionState(state);
        return new NotificationContent(formattedTitle, lastAction);
    }

    private static String formatConnectionState(ConnectionState state) {
        switch (state) {
            case CONNECTED:
                return "Connected";
            case CONNECTING:
                return "Connecting...";
            case DISCONNECTED:
                return "Disconnected";
            case DISCONNECTING:
                return "Disconnecting...";
            case FAILED:
                return "Connection Failed";
            default:
                return "Unknown State";
        }
    }
}
