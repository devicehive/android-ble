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
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import timber.log.Timber;

/**
 * Created by Constantine Mars on 3/27/15.
 * Provides single callback for gatt with configurable actions
 */
public class InteractiveGattCallback extends BluetoothGattCallback {
    private boolean servicesDiscovered = false;
    private String address;
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

    public InteractiveGattCallback(String address, SimpleCallableFuture<CommandResult> future, Context context, DisconnecListener disconnecListener) {
        this.address = address;
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
        readOperation = new ReadCharacteristicOperation(address, serviceUUID, characteristicUUID, callBack, future, context);
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
        writeOperation = new WriteCharacteristicOperation(address, serviceUUID, characteristicUUID, callBack, value, callableFuture, context);
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

        public ReadCharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(device, serviceUUID, characteristicUUID, callBack, callableFuture, context);
        }

        @Override
        protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (!gatt.readCharacteristic(characteristic)) {
                callableFuture.call(commandResultFail());
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = characteristic.getValue();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                callBack.onRead(value);
                callableFuture.call(commandResultSuccessWithValue(value));
                return;
            }

//          TODO: handle BluetoothGatt.GATT_WRITE_NOT_PERMITTED and others
            callableFuture.call(commandResultFailWithStatusAndValue(String.valueOf(status), value));
        }
    }

    public static class WriteCharacteristicOperation extends CharacteristicOperation {
        private byte[] value;

        public WriteCharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, byte[] value, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(device, serviceUUID, characteristicUUID, callBack, callableFuture, context);
            this.value = value;
        }

        @Override
        protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            characteristic.setValue(value);
            if (!gatt.writeCharacteristic(characteristic)) {
                callableFuture.call(commandResultFail());
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            callBack.onWrite(status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callableFuture.call(commandResultSuccess());
            } else {
//                TODO: handle BluetoothGatt.GATT_WRITE_NOT_PERMITTED and others
                callableFuture.call(commandResultFailWithStatusAndValue(String.valueOf(status), value));
            }
        }
    }

    public abstract static class CharacteristicOperation {
        private String serviceUUID;
        private String characteristicUUID;
        protected String device;
        protected GattCharacteristicCallBack callBack;
        protected SimpleCallableFuture<CommandResult> callableFuture;
        protected Context context;

        public CharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            this.device = device;
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

            callableFuture.call(commandResultFailWithStatus(context.getString(R.string.status_json_not_found)));
        }

        abstract protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        abstract public void onResult(BluetoothGattCharacteristic characteristic, int status);

        private String jsonStatus(int statusStringId) {
            return jsonStatus(context.getString(statusStringId));
        }

        private String jsonStatus(String status) {
            return new Gson().toJson(new StatusJson.Status(
                    status,
                    device,
                    serviceUUID,
                    characteristicUUID
            ));
        }

        private String jsonStatusWithValue(String status, byte[] value) {
            return new Gson().toJson(new StatusJson.StatusWithValue(
                    status,
                    value,
                    device,
                    serviceUUID,
                    characteristicUUID
            ));
        }

        private String jsonStatusWithValue(int statusStringId, byte[] value) {
            return jsonStatusWithValue(context.getString(statusStringId), value);
        }

        protected CommandResult commandResultSuccessWithValue(byte[] value) {
            return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatusWithValue(R.string.status_json_success, value));
        }

        protected CommandResult commandResultSuccess() {
            return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatus(R.string.status_json_success));
        }

        protected CommandResult commandResultFailWithStatus(String status) {
            return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(status));
        }

        protected CommandResult commandResultFail() {
            return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(R.string.status_json_fail));
        }

        protected CommandResult commandResultFailWithStatusAndValue(String status, byte[] value) {
            return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusWithValue(status, value));
        }

        protected CommandResult commandResultFailWithValue(byte[] value) {
            return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusWithValue(R.string.status_json_fail, value));
        }

        protected CommandResult commandResultNotFound() {
            return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(R.string.status_json_not_found));
        }
    }

    public interface DisconnecListener {
        void onDisconnect();
    }
}
