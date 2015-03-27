package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.dataart.btle_android.btle_gateway.GattCharacteristicCallBack;

/**
 * Created by Constantine Mars on 3/26/15.
 * read callback
 */
public class ReadGattCallback extends BaseGattCallback {
    public ReadGattCallback(GattCharacteristicCallBack gattCharacteristicCallBack, String serviceUUID, String characteristicUUID) {
        super(gattCharacteristicCallBack, serviceUUID, characteristicUUID,
                new CharacteristicOperation() {
            @Override
            public void call(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                gatt.readCharacteristic(characteristic);
            }
        });
    }
}