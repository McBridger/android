package com.bridger;

import com.bridger.events.ClipboardEvent;
import com.bridger.events.SystemEvent;
import com.bridger.model.ConnectionState;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class Store {

    private static volatile Store instance;

    // State subjects
    private final BehaviorSubject<ConnectionState> connectionStateSubject = BehaviorSubject.createDefault(ConnectionState.DISCONNECTED);
    private final BehaviorSubject<String> lastActionSubject = BehaviorSubject.createDefault("No action yet.");

    // Event subjects
    private final PublishSubject<ClipboardEvent> clipboardEventSubject = PublishSubject.create();
    private final PublishSubject<SystemEvent> systemEventSubject = PublishSubject.create(); // New subject for system events

    private Store() {
        // Private constructor to enforce Singleton pattern
    }

    public static Store getInstance() {
        if (instance == null) {
            synchronized (Store.class) {
                if (instance == null) {
                    instance = new Store();
                }
            }
        }
        return instance;
    }

    // Getters for state subjects (as Observables to prevent external emission)
    public BehaviorSubject<ConnectionState> getConnectionStateSubject() {
        return connectionStateSubject;
    }

    public BehaviorSubject<String> getLastActionSubject() {
        return lastActionSubject;
    }

    // Getter for event subject (as Observable to prevent external emission)
    public PublishSubject<ClipboardEvent> getClipboardEventSubject() {
        return clipboardEventSubject;
    }

    public PublishSubject<SystemEvent> getSystemEventSubject() {
        return systemEventSubject;
    }

    // Actions (methods to update state or dispatch events)
    public void updateConnectionState(ConnectionState newState) {
        connectionStateSubject.onNext(newState);
    }

    public void updateLastAction(String action) {
        lastActionSubject.onNext(action);
    }

    public void dispatchClipboardEvent(ClipboardEvent event) {
        clipboardEventSubject.onNext(event);
    }

    public void dispatchSystemEvent(SystemEvent event) {
        systemEventSubject.onNext(event);
    }
}
