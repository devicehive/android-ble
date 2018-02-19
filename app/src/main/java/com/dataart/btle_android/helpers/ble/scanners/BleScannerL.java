package com.dataart.btle_android.helpers.ble.scanners;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.os.Build;

/**
 * Created by Constantine Mars on 6/13/16.
 * <p>
 * Scanner for Android L and above
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleScannerL extends BleScannerJ {

    private BluetoothLeScanner scanner;

    private final android.bluetooth.le.ScanCallback callback;

    public BleScannerL(ScanCallback scanCallback, BluetoothAdapter bluetoothAdapter) {
        super(scanCallback, bluetoothAdapter);

        callback = createCallback(scanCallback);
    }

    private static android.bluetooth.le.ScanCallback createCallback(ScanCallback scanCallback) {
        return new android.bluetooth.le.ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                BluetoothDevice device = result.getDevice();
                int rssi = result.getRssi();
                byte[] scanRecord = (result.getScanRecord() != null ? result.getScanRecord().getBytes() : null);
                scanCallback.onDeviceFound(device, rssi, scanRecord);

                super.onScanResult(callbackType, result);
            }
        };
    }

    private BluetoothLeScanner getScanner() {
        if (scanner == null && bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return scanner;
    }

    @Override
    public void startScan() {
        getScanner().startScan(callback);
    }

    @Override
    public void stopScan() {
        getScanner().stopScan(callback);
    }


}
