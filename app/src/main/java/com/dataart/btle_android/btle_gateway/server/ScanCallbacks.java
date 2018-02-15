package com.dataart.btle_android.btle_gateway.server;

import android.bluetooth.BluetoothDevice;

import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.R;
import com.dataart.btle_android.helpers.BleHelpersFactory;
import com.dataart.btle_android.helpers.ble.base.BleScanner;

import timber.log.Timber;

class ScanCallbacks {
    private ConnectionOperation operation;
    private BleScanner.ScanCallback localCallback;
    private BleScanner scanner;
    private BluetoothServer server;

    public ScanCallbacks(BluetoothServer server, final String address, final ConnectionOperation operation) {
        this.operation = operation;
        this.server = server;

        this.localCallback = new BleScanner.ScanCallback() {
            private boolean found = false;

            @Override
            public void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (!found && device.getAddress().equals(address)) {
                    found = true;
                    stop();
                    onDeviceFound(device, rssi, scanRecord);

                    server.connectAndSave(address, device, () -> {
                        Timber.d("on connected - calling operation");
                        server.applyForConnection(address, operation);
                    });
                }
            }
        };
    }

    public void start() {
        Timber.d("no device or connection - scanning for device");
//              startScan for device, add it to discovered devices, connect and call operation

        scanner = BleHelpersFactory.getScanner(localCallback, server.getBluetoothAdapter());
        assert scanner != null;
        scanner.startScan();

        ScanCallbacks sc = this;

        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(BluetoothServer.COMMAND_SCAN_DELAY);
                    //              "Never startScan on a loop, and set a time limit on your startScan. " - https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
                    Timber.d("on timeout");
                    sc.stop();
                    operation.fail(BTLEApplication.getApplication().getString(R.string.status_notfound_timeout));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void stop() {
        Timber.d("stop");
        scanner.stopScan();
    }
}
