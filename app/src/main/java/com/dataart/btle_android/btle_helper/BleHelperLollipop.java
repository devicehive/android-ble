package com.dataart.btle_android.btle_helper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Data;

/**
 * Created by Constantine Mars on 12/6/15.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@Data
public class BleHelperLollipop extends BleHelper {

    private BluetoothLeScanner scanner;
    private boolean stopped = false;
    private ScanCallback callback = new ScanCallback() {
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

    public BleHelperLollipop(Activity activity, ScanListener listener) {
        super(activity, listener);
        init();
    }

    public void scan(boolean enable) {
        if (scanner == null && adapter != null) {
            scanner = adapter.getBluetoothLeScanner();
        }
        if (scanner != null) {
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


}
