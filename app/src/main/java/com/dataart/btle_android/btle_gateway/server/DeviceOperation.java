package com.dataart.btle_android.btle_gateway.server;

import android.bluetooth.BluetoothDevice;

interface DeviceOperation {
    void call(BluetoothDevice device);

    void fail(String message);
}
