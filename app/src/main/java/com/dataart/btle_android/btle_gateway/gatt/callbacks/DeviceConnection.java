package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

/**
 * Created by Constantine Mars on 3/27/15.
 * structure to store single gatt connection callback in one place
 */
public class DeviceConnection {
    private String address;
    private BluetoothGatt gatt;
    private InteractiveGattCallback callback;

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
