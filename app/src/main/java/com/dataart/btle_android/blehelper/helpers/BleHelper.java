package com.dataart.btle_android.blehelper.helpers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import timber.log.Timber;

/**
 * Created by Constantine Mars on 12/13/15.
 */
@Data
@RequiredArgsConstructor
public abstract class BleHelper {
    public static final int SCAN_PERIOD = 10;
    private static final int REQUEST_ENABLE_BT = 3001;
    protected final Activity activity;
    protected final ScanListener listener;
    protected BluetoothAdapter adapter;
    protected Map<BluetoothDevice, Integer> devices = new HashMap<>();
    protected boolean scanning = false;

    public void init() {
        final BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();

        checkBluetoothState();
    }

    private boolean checkBluetoothState() {
        if (adapter == null) {
            Timber.i("Bluetooth is not supported");
            return false;
        }
        if (!adapter.isEnabled()) {
            Timber.i("Bluetooth is disabled. Enabling...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        Timber.i("Bluetooth enabled");
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                checkBluetoothState();
                break;
        }
    }

    public void addDevice(BluetoothDevice device, int rssi) {
        devices.put(device, rssi);
    }

    public abstract void scan(boolean enable);

    public interface ScanListener {
        void onCompleted(Map<BluetoothDevice, Integer> devices);
    }


}
