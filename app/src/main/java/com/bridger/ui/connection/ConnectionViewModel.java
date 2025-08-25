package com.bridger.ui.connection;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bridger.ClipboardUtility; // Use our new ClipboardUtility
import com.bridger.Store; // Import the Store
import com.bridger.events.ClipboardEvent;
import com.bridger.model.ConnectionState; // Import ConnectionState from model

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConnectionViewModel extends AndroidViewModel {

    private static final String TAG = "ConnectionViewModel";

    private final Store store;
    private final ClipboardUtility clipboardUtility; // Use our new ClipboardUtility
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<List<String>> clipboardHistory = new MutableLiveData<>();

    public ConnectionViewModel(@NonNull Application application) {
        super(application);
        this.store = Store.getInstance(); // Get Store instance
        this.clipboardUtility = ClipboardUtility.getInstance(application.getApplicationContext()); // Get ClipboardUtility instance
        clipboardHistory.setValue(new ArrayList<>());
        observeStoreState(); // Observe state from Store
        observeReceivedEvents(); // Observe RECEIVED events from Store
        observeSentEvents(); // Observe SENT events from Store
    }

    // Expose LiveData from the Store's BehaviorSubjects
    public LiveData<ConnectionState> getConnectionState() {
        return new LiveData<>() {
            @Override
            protected void onActive() {
                super.onActive();
                disposables.add(store.connection
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::setValue,
                                throwable -> Log.e(TAG, "Error observing connection state from Store: " + throwable.getMessage())));
            }

            @Override
            protected void onInactive() {
                super.onInactive();
                // Disposables are cleared in onCleared, so no need to clear here specifically for LiveData
            }
        };
    }

    public LiveData<String> getLastAction() {
        return new LiveData<>() {
            @Override
            protected void onActive() {
                super.onActive();
                disposables.add(store.lastAction
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::setValue,
                                throwable -> Log.e(TAG, "Error observing last action from Store: " + throwable.getMessage())));
            }

            @Override
            protected void onInactive() {
                super.onInactive();
            }
        };
    }

    public LiveData<List<String>> getClipboardHistory() {
        return clipboardHistory;
    }

    private void observeStoreState() {
        // No direct observation needed here for connectionState and lastAction, as LiveData wrappers handle it.
        // This method can be used for other state observations if needed.
    }

    private void observeReceivedEvents() {
        disposables.add(store.clipboard
                .filter(event -> event.getType() == ClipboardEvent.EventType.RECEIVED)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> {
                            Log.d(TAG, "Received clipboard event from Store: " + event.getData());
                            clipboardUtility.writeToClipboard(event.getData()); // Write to system clipboard
                            addTextToHistory("Received: " + event.getData());
                        },
                        throwable -> Log.e(TAG, "Error observing RECEIVED clipboard events from Store: " + throwable.getMessage())
                ));
    }

    private void observeSentEvents() {
        disposables.add(store.clipboard
                .filter(event -> event.getType() == ClipboardEvent.EventType.SENT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> {
                            Log.d(TAG, "Sent clipboard event acknowledged by Store: " + event.getData());
                            addTextToHistory("Sent: " + event.getData());
                        },
                        throwable -> Log.e(TAG, "Error observing SENT clipboard events from Store: " + throwable.getMessage())
                ));
    }

    private void addTextToHistory(String text) {
        List<String> currentHistory = clipboardHistory.getValue();
        if (currentHistory == null) {
            currentHistory = new ArrayList<>();
        }
        currentHistory.add(0, text);
        clipboardHistory.postValue(currentHistory);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        // ClipboardUtility no longer has RxJava subscriptions to dispose of
    }
}
