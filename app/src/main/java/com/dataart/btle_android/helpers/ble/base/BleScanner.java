package com.dataart.btle_android.helpers.ble.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Created by Constantine Mars on 6/13/16.
 * Helper for BLE scanner. Can be singleton and accessible from android services
 */

@Data
@RequiredArgsConstructor
public abstract class BleScanner {
    protected final ScanCallback scanCallback;
    protected final BluetoothAdapter bluetoothAdapter;

    public abstract void startScan();

    public abstract void stopScan();

    public interface ScanCallback {
        void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);
    }
}
