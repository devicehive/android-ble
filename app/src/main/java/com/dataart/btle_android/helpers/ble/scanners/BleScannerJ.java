package com.dataart.btle_android.helpers.ble.scanners;

import android.bluetooth.BluetoothAdapter;

import com.dataart.btle_android.helpers.ble.base.BleScanner;


/**
 * Created by Constantine Mars on 6/13/16.
 * Scanner for Android Jelly Bean
 */

@SuppressWarnings("deprecation")
public class BleScannerJ extends BleScanner {

    private final BluetoothAdapter.LeScanCallback callback;

    public BleScannerJ(ScanCallback scanCallback, BluetoothAdapter bluetoothAdapter) {
        super(scanCallback, bluetoothAdapter);
        callback = scanCallback::onDeviceFound;
    }

    @Override
    public void startScan() {
        bluetoothAdapter.startLeScan(callback);
    }

    @Override
    public void stopScan() {
        bluetoothAdapter.stopLeScan(callback);
    }
}
