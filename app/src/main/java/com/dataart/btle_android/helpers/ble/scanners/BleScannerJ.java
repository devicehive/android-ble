package com.dataart.btle_android.helpers.ble.scanners;

import android.bluetooth.BluetoothAdapter;

import com.dataart.btle_android.helpers.ble.base.BleScanner;

import java.util.concurrent.TimeUnit;


/**
 * Created by Constantine Mars on 6/13/16.
 * Scanner for Android Jelly Bean
 */

@SuppressWarnings("deprecation")
public class BleScannerJ extends BleScanner {

    private BluetoothAdapter.LeScanCallback callback = (device, rssi, scanRecord) -> addDevice(device, rssi);

    public BleScannerJ(ScanCallback listener, BluetoothAdapter bluetoothAdapter) {
        super(listener, bluetoothAdapter);
    }

    @Override
    public void scan(boolean enable) {
        if (bluetoothAdapter != null) {
            if (enable) {
                scanning = true;
                bluetoothAdapter.startLeScan(callback);

                rx.Observable.timer(SCAN_PERIOD, TimeUnit.SECONDS).forEach(aLong -> {
                    scanning = false;
                    bluetoothAdapter.stopLeScan(callback);
                    listener.onCompleted(devices);
                });
            } else {
                scanning = false;
                bluetoothAdapter.stopLeScan(callback);
                listener.onCompleted(devices);
            }
        }
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
