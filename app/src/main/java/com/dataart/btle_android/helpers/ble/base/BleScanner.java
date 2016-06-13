package com.dataart.btle_android.helpers.ble.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Created by Constantine Mars on 6/13/16.
 * Helper for BLE scanner. Can be singleton and accessible from android services
 */

@Data
@RequiredArgsConstructor
public abstract class BleScanner {
    protected static final long SCAN_PERIOD = 10;

    protected final ScanCallback listener;
    protected final BluetoothAdapter bluetoothAdapter;

    protected Map<BluetoothDevice, Integer> devices = new HashMap<>();
    protected boolean scanning = false;

    public abstract void scan(boolean enable);

    protected void addDevice(BluetoothDevice device, int rssi) {
        devices.put(device, rssi);
    }

    public interface ScanCallback {
        void onCompleted(Map<BluetoothDevice, Integer> devices);
    }
}
