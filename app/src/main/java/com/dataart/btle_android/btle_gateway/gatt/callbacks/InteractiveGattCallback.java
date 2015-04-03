package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.GattCharacteristicCallBack;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.dataart.btle_android.btle_gateway.Utils;
import com.google.gson.Gson;

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
    private DisconnecListener disconnecListener;
    private boolean connectionStateChanged = false;
    public boolean isConnectionStateChanged() {
        return connectionStateChanged;
    }

    public InteractiveGattCallback(SimpleCallableFuture<CommandResult> future, Context context, DisconnecListener disconnecListener) {
        this.callableFuture = future;
        this.context = context;
        this.disconnecListener = disconnecListener;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        connectionStateChanged = true;

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Timber.d("isConnectionStateChanged. discovering services");
            this.gatt = gatt;
            this.gatt.discoverServices();
            if (!callableFuture.isGetDone()) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_COMLETED, context.getString(R.string.status_ok)));
            }
        } else {
            String m = String.format(context.getString(R.string.connection_failed_result), status, newState);
            Timber.d(m);
            if (!callableFuture.isGetDone()) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
            }
            disconnecListener.onDisconnect();
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

        Timber.d("gatt is null - probably not isConnectionStateChanged");
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
            byte[] value = characteristic.getValue();
            callBack.onRead(value);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final String sValue = Utils.printHexBinary(value);
                callableFuture.call(new CommandResult(CommandResult.STATUS_COMLETED, String.format(context.getString(R.string.value), sValue)));
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
                callableFuture.call(new CommandResult(CommandResult.STATUS_COMLETED, String.format(context.getString(R.string.status), status)));
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
                } else {
                    noSuchCharacteristic(context, characteristicUUID, serviceUUID, callableFuture);
                }
            } else {
                noSuchService(context, serviceUUID, callableFuture);
            }
            String m = String.format(context.getString(R.string.read_ch_failed), characteristicUUID, serviceUUID);
            Timber.d(m);
            callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
        }

        private static void noSuchCharacteristic(Context context, String characteristicUUID, String serviceUUID, SimpleCallableFuture<CommandResult> callableFuture) {
            String m = String.format(context.getString(R.string.no_such_characteristic), characteristicUUID, serviceUUID);
            Timber.d(m);
            callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
        }

        private static void noSuchService(Context context, String serviceUUID, SimpleCallableFuture<CommandResult> callableFuture) {
            String m = String.format(context.getString(R.string.no_such_service), serviceUUID);
            Timber.d(m);
            callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
        }

        abstract protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        abstract public void onResult(BluetoothGattCharacteristic characteristic, int status);
    }

    public interface DisconnecListener {
        void onDisconnect();
    }
}
