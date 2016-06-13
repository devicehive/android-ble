package com.dataart.btle_android.helpers.ble.scanners;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.os.Build;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Constantine Mars on 6/13/16.
 * <p>
 * Scanner for Android L and above
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleScannerL extends BleScannerJ {
    private BluetoothLeScanner scanner;
    private boolean stopped = false;
    private android.bluetooth.le.ScanCallback callback = new android.bluetooth.le.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            addDevice(device, rssi);
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public BleScannerL(ScanCallback listener, BluetoothAdapter bluetoothAdapter) {
        super(listener, bluetoothAdapter);
    }

    private BluetoothLeScanner getScanner() {
        if (scanner == null && bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return scanner;
    }

    public void scan(boolean enable) {
        if (getScanner() != null) {
            if (enable) {
                scanning = true;
                scanner.startScan(callback);

                rx.Observable.timer(SCAN_PERIOD, TimeUnit.SECONDS).forEach(aLong -> {
                    if (!stopped) {
                        scanner.stopScan(callback);
                        listener.onCompleted(devices);
                        scanning = false;
                    }
                    stopped = false;
                });
            } else {
                scanner.stopScan(callback);
                listener.onCompleted(devices);
                scanning = false;
                stopped = true;
            }
        }
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
