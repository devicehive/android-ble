package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import com.dataart.btle_android.btle_gateway.GattCharacteristicCallBack;

import java.util.UUID;

/**
 * Created by Constantine Mars on 3/26/15.
 * base callback implementation. use CharacteristicOperation for custom operation with characteristic
 */
public class BaseGattCallback extends BluetoothGattCallback {
    protected GattCharacteristicCallBack gattCharacteristicCallBack;
    protected String serviceUUID;
    protected String characteristicUUID;
    private CharacteristicOperation action;

    public BaseGattCallback(GattCharacteristicCallBack gattCharacteristicCallBack, String serviceUUID, String characteristicUUID, CharacteristicOperation action) {
        this.gattCharacteristicCallBack = gattCharacteristicCallBack;
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.action = action;
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        gattCharacteristicCallBack.onRead(characteristic.getValue());
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        final BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
        if (service != null) {
            final BluetoothGattCharacteristic characteristic
                    = service.getCharacteristic(UUID.fromString(characteristicUUID));
            if (characteristic != null && action != null) {
                action.call(gatt, characteristic);
            }
        }
    }

    public interface CharacteristicOperation {
        void call(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    }
}