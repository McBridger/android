package com.bridger;

import com.bridger.events.ClipboardEvent;
import com.bridger.events.SystemEvent;
import com.bridger.model.ConnectionState;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class Store {

    private static volatile Store instance;

    // State subjects
    public final BehaviorSubject<ConnectionState> connection = BehaviorSubject.createDefault(ConnectionState.DISCONNECTED);
    public final BehaviorSubject<String> lastAction = BehaviorSubject.createDefault("No action yet.");

    // Event subjects
    public final PublishSubject<ClipboardEvent> clipboard = PublishSubject.create();
    public final PublishSubject<SystemEvent> system = PublishSubject.create(); // New subject for system events

    private Store() {}

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
}
