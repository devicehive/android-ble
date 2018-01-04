package com.dataart.btle_android.btle_gateway.server;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.R;
import com.dataart.btle_android.helpers.BleHelpersFactory;
import com.dataart.btle_android.helpers.ble.base.BleScanner;

import timber.log.Timber;

class ScanCallbacks {
    private String address;
    private ConnectionOperation operation;
    private BleScanner.ScanCallback localCallback;
    private BleScanner scanner;
    private BluetoothServer server;

    public ScanCallbacks(BluetoothServer server, final String address, final ConnectionOperation operation) {
        this.address = address;
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
        scanner.startScan();

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
//              "Never startScan on a loop, and set a time limit on your startScan. " - https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
            Timber.d("on timeout");
            stop();
            operation.fail(BTLEApplication.getApplication().getString(R.string.status_notfound_timeout));
        }, BluetoothServer.COMMAND_SCAN_DELAY);
    }

    public void stop() {
        Timber.d("stop");
        scanner.stopScan();
    }
}
