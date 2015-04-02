package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.GattCharacteristicCallBack;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;

import java.util.UUID;

import timber.log.Timber;

/**
 * Created by Constantine Mars on 3/27/15.
 * Provides single callback for gatt with configurable actions
 */
public class InteractiveGattCallback extends BluetoothGattCallback {
    private boolean servicesDiscovered = false;
    private BluetoothGatt gatt;
    private ReadCharacteristicOperation readOperation;
    private WriteCharacteristicOperation writeOperation;
    private SimpleCallableFuture<CommandResult> callableFuture;
    private Context context;

    public InteractiveGattCallback(SimpleCallableFuture<CommandResult> future, Context context) {
        this.callableFuture = future;
        this.context = context;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Timber.d("connected. discovering services");
            this.gatt = gatt;
            this.gatt.discoverServices();
            callableFuture.call(new CommandResult(CommandResult.STATUS_COMLETED, "Ok"));
        } else {
            Timber.d("connection state:" + newState);
            callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, "Failed with status=" + status + ", state=" + newState));
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        servicesDiscovered = true;
        if (readOperation!=null) {
            readOperation.call(gatt);
        }
        if (writeOperation!=null) {
            writeOperation.call(gatt);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (readOperation !=null) {
            readOperation.onResult(characteristic, status);
//            Reset readOperation for future calls
            readOperation = null;
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (writeOperation!=null) {
            writeOperation.onResult(characteristic, status);
            writeOperation = null;
        }
    }

    public void readCharacteristic(String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> future) {
        readOperation = new ReadCharacteristicOperation(serviceUUID, characteristicUUID, callBack, future, context);
        if (gatt != null) {
            if (servicesDiscovered) {
//                read right now
                readOperation.call(gatt);
            } else {
//                discoverServices should be called before r/w operations - so we postpone call to readOperation
                gatt.discoverServices();
            }
            return;
        }

        Timber.d("gatt is null - probably not connected");
    }

    public void writeCharacteristic(String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, byte[] value, SimpleCallableFuture<CommandResult> callableFuture) {
        writeOperation = new WriteCharacteristicOperation(serviceUUID, characteristicUUID, callBack, value, callableFuture, context);
        if (gatt != null) {
            if (servicesDiscovered) {
//                read right now
                writeOperation.call(gatt);
            } else {
//                discoverServices should be called before r/w operations - so we postpone call to readOperation
                gatt.discoverServices();
            }
            return;
        }

        String m = context.getString(R.string.gatt_null);
        Timber.d(m);
        callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
    }

    public static class ReadCharacteristicOperation extends CharacteristicOperation {

        public ReadCharacteristicOperation(String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(serviceUUID, characteristicUUID, callBack, callableFuture, context);
        }

        @Override
        protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (!gatt.readCharacteristic(characteristic)) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, context.getString(R.string.status_fail)));
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            callBack.onRead(characteristic.getValue());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_COMLETED, context.getString(R.string.status_ok)));
            } else {
//                TODO: handle BluetoothGatt.GATT_WRITE_NOT_PERMITTED and others
                callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, context.getString(R.string.gatt_failure)+status));
            }
        }
    }

    public static class WriteCharacteristicOperation extends CharacteristicOperation {
        private byte[] value;

        public WriteCharacteristicOperation(String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, byte[] value, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(serviceUUID, characteristicUUID, callBack, callableFuture, context);
            this.value = value;
        }

        @Override
        protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            characteristic.setValue(value);
            if (!gatt.writeCharacteristic(characteristic)) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, context.getString(R.string.status_fail)));
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            callBack.onWrite(status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_COMLETED, context.getString(R.string.status_ok)));
            } else {
//                TODO: handle BluetoothGatt.GATT_WRITE_NOT_PERMITTED and others
                callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, context.getString(R.string.gatt_failure)+status));
            }
        }
    }

    public abstract static class CharacteristicOperation {
        private String serviceUUID;
        private String characteristicUUID;
        protected GattCharacteristicCallBack callBack;
        protected SimpleCallableFuture<CommandResult> callableFuture;
        protected Context context;

        public CharacteristicOperation(String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            this.serviceUUID = serviceUUID;
            this.characteristicUUID = characteristicUUID;
            this.callBack = callBack;
            this.callableFuture = callableFuture;
            this.context = context;
        }

        public void call(BluetoothGatt gatt) {
            BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    request(gatt, characteristic);
                    return;
                }
            }
            String m = String.format(context.getString(R.string.read_ch_failed), characteristicUUID, serviceUUID);
            Timber.d(m);
            callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
        }

        abstract protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        abstract public void onResult(BluetoothGattCharacteristic characteristic, int status);
    }
}
