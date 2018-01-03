package com.dataart.btle_android.btle_gateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import java.util.List;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;

/**
 * Created by Constantine Mars on 6/3/16.
 * Abstracted BTLE scanning manager that encapsulates both approaches for android before L and starting with L
 */

public class BTLEScannerOld {


    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothAdapter.LeScanCallback leScanCallback;
    private final android.bluetooth.le.ScanCallback scanCallback;
    private final BluetoothLeScanner leScanner;

    public BTLEScannerOld(BluetoothAdapter bluetoothAdapter, Callback callback) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.leScanCallback = callback::onScan;

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            this.leScanner = bluetoothAdapter.getBluetoothLeScanner();

            this.scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        callback.onScan(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                    }

                    super.onScanResult(callbackType, result);
                }
            };
        } else {
            this.leScanner = null;
            this.scanCallback = null;
        }
    }

    public void startScan() {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            leScanner.startScan(new android.bluetooth.le.ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {

                    super.onScanResult(callbackType, result);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }
            });
        } else {
            bluetoothAdapter.startLeScan(leScanCallback);
        }
    }

    public void stopScan() {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            leScanner.stopScan(this.scanCallback);
        } else {
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    interface Callback {
        void onScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    }

}
