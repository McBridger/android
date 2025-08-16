package com.bridger.model;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

import no.nordicsemi.android.ble.data.Data;

public class Characteristic {
    @NonNull
    public final UUID uuid;
    @Nullable
    public BluetoothGattCharacteristic gattCharacteristic;
    @Nullable
    public final Consumer<Data> notificationCallback;

    public Characteristic(@NonNull UUID uuid, @Nullable Consumer<Data> notificationCallback) {
        this.uuid = uuid;
        this.notificationCallback = notificationCallback;
    }

    public Characteristic(@NonNull UUID uuid) {
        this(uuid, null);
    }
}
