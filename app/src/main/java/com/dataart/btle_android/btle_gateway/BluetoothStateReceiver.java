package com.dataart.btle_android.btle_gateway;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by idyuzheva
 */
public abstract class BluetoothStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                    onBluetoothOff();
                    break;
                case BluetoothAdapter.STATE_ON:
                    onBluetoothOn();
                    break;
            }
        }
    }

    protected abstract void onBluetoothOff();

    protected abstract void onBluetoothOn();
}