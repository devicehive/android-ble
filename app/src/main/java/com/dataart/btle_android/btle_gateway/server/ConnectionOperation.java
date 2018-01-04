package com.dataart.btle_android.btle_gateway.server;

import com.dataart.btle_android.btle_gateway.gatt_callbacks.DeviceConnection;

interface ConnectionOperation {
    void call(DeviceConnection connection);

    void fail(String message);
}
