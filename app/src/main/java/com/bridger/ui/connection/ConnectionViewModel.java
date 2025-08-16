package com.bridger.ui.connection;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bridger.BleConnectionManager;
import com.bridger.events.ClipboardEvent; // Import ClipboardEvent

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConnectionViewModel extends AndroidViewModel {

    private static final String TAG = "ConnectionViewModel";

    private final BleConnectionManager bleConnectionManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<List<String>> clipboardHistory = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<BleConnectionManager.ConnectionState> connectionStateLiveData = new MutableLiveData<>();


    public ConnectionViewModel(@NonNull Application application) {
        super(application);
        bleConnectionManager = BleConnectionManager.getInstance(application.getApplicationContext());
        clipboardHistory.setValue(new ArrayList<>());
        observeBleConnectionState();
        observeClipboardEvents(); // New method to observe clipboard events
    }

    public LiveData<BleConnectionManager.ConnectionState> getConnectionState() {
        return connectionStateLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<List<String>> getClipboardHistory() {
        return clipboardHistory;
    }

    private void observeBleConnectionState() {
        disposables.add(bleConnectionManager.getConnectionState()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        connectionState -> {
                            Log.d(TAG, "Connection state changed: " + connectionState);
                            connectionStateLiveData.postValue(connectionState);
                        },
                        throwable -> {
                            Log.e(TAG, "Error observing connection state", throwable);
                            errorMessage.postValue("Connection error: " + throwable.getMessage());
                        }
                ));
    }

    private void observeClipboardEvents() {
        disposables.add(bleConnectionManager.clipboardEvents
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> {
                            if (event instanceof ClipboardEvent.Receive) {
                                ClipboardEvent.Receive receiveEvent = (ClipboardEvent.Receive) event;
                                Log.d(TAG, "Received clipboard event: " + receiveEvent.text);
                                updateAndroidClipboard(receiveEvent.text);
                                addTextToHistory("Received: " + receiveEvent.text);
                            } else if (event instanceof ClipboardEvent.Send) {
                                ClipboardEvent.Send sendEvent = (ClipboardEvent.Send) event;
                                Log.d(TAG, "Sent clipboard event acknowledged: " + sendEvent.text);
                                addTextToHistory("Sent: " + sendEvent.text);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error observing clipboard events", throwable);
                            errorMessage.postValue("Clipboard event error: " + throwable.getMessage());
                        }
                ));
    }

    public void sendClipboard(String text) {
        bleConnectionManager.clipboardEvents.onNext(new ClipboardEvent.Send(text));
        Log.d(TAG, "Queued clipboard for sending: " + text);
    }

    private void updateAndroidClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Bridger Clipboard", text);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Android clipboard updated: " + text);
        }
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
    }
}
