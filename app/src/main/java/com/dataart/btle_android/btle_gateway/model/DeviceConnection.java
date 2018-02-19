package com.dataart.btle_android.btle_gateway.model;

import android.bluetooth.BluetoothGatt;

import com.dataart.btle_android.btle_gateway.gatt_callbacks.InteractiveGattCallback;

/**
 * Created by Constantine Mars on 3/27/15.
 * Structure to store single gatt connection callback in one place
 */
public class DeviceConnection {
    private final String address;
    private final BluetoothGatt gatt;
    private final InteractiveGattCallback callback;

    public DeviceConnection(String address, BluetoothGatt gatt, InteractiveGattCallback callback) {
        this.address = address;
        this.gatt = gatt;
        this.callback = callback;
    }

    public String getAddress() {
        return address;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public InteractiveGattCallback getCallback() {
        return callback;
    }
}
