package com.dataart.btle_android.btle_gateway.server;

import android.bluetooth.BluetoothDevice;

interface DiscoveredDeviceListener {
    void onDiscoveredDevice(BluetoothDevice device);
}
