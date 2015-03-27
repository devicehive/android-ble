package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.dataart.btle_android.btle_gateway.GattCharacteristicCallBack;

/**
 * Created by Constantine Mars on 3/26/15.
 * write callback
 */
public class WriteGattCallback extends BaseGattCallback {

    public WriteGattCallback(GattCharacteristicCallBack gattCharacteristicCallBack, String serviceUUID, String characteristicUUID, final byte[] value) {
        super(gattCharacteristicCallBack, serviceUUID, characteristicUUID,
                new CharacteristicOperation() {
                    @Override
                    public void call(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        characteristic.setValue(value);
                        gatt.writeCharacteristic(characteristic);
                    }
                });
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        gattCharacteristicCallBack.onWrite(status);
    }
}