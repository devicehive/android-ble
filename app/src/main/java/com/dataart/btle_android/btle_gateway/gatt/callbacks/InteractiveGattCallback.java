package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.BluetoothServer;
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
    private String address;
    private BluetoothGatt gatt;
    private ReadCharacteristicOperation readOperation;
    private WriteCharacteristicOperation writeOperation;
    private NotificaitonSubscription notificaitonSubscription;
    private ServicesDiscoveredCallback servicesDiscoveredCallback;
    private CharacteristicsDiscoveringCallback characteristicsDiscoveringCallback;
    private SimpleCallableFuture<CommandResult> callableFuture;
    private Context context;
    private DisconnecListener disconnecListener;
    private boolean connectionStateChanged = false;

    public boolean isConnectionStateChanged() {
        return connectionStateChanged;
    }

    public void setServicesDiscoveredCallback(ServicesDiscoveredCallback servicesDiscoveredCallback) {
        this.servicesDiscoveredCallback = servicesDiscoveredCallback;
    }

    public void setCharacteristicsDiscoveringCallback(CharacteristicsDiscoveringCallback characteristicsDiscoveringCallback) {
        this.characteristicsDiscoveringCallback = characteristicsDiscoveringCallback;
    }

    public void setNotificaitonSubscription(NotificaitonSubscription notificaitonSubscription) {
        this.notificaitonSubscription = notificaitonSubscription;
        if (servicesDiscovered) {
            this.notificaitonSubscription.subscribe(gatt);
            return;
        }

        gatt.discoverServices();
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

            if (callableFuture!=null && !callableFuture.isGetDone()) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_COMLETED, context.getString(R.string.status_ok)));
            }
        } else {
            String m = String.format(context.getString(R.string.connection_failed_result), status, newState);
            Timber.d(m);
            if (callableFuture!=null && !callableFuture.isGetDone()) {
                callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
            }
            if (disconnecListener!=null) {
                disconnecListener.onDisconnect();
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        servicesDiscovered = true;

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (readOperation!=null){
                readOperation.call(gatt);
            }
            if (writeOperation!=null){
                writeOperation.call(gatt);
            }
            if (servicesDiscoveredCallback!=null){
                servicesDiscoveredCallback.call(gatt);
            }
            if (characteristicsDiscoveringCallback!=null){
                characteristicsDiscoveringCallback.call(gatt);
            }
            if (notificaitonSubscription!=null){
                notificaitonSubscription.subscribe(gatt);
//                unsubscribe and don't listen for future notifications
                if (!notificaitonSubscription.isOn()){
                    notificaitonSubscription=null;
                }
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Timber.d("onCharacteristicRead. notificaitonSubscription="+(notificaitonSubscription!=null?1:0));
        if (readOperation!=null){
            readOperation.onResult(characteristic, status);
//            Reset readOperation for future calls
            readOperation=null;
        }
        if (notificaitonSubscription!=null){
            notificaitonSubscription.onNotification(characteristic.getValue());
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Timber.d("onCharacteristicChanged. notificaitonSubscription="+(notificaitonSubscription!=null?1:0));
        if (notificaitonSubscription!=null){
            notificaitonSubscription.onNotification(characteristic.getValue());
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
        if (callableFuture!=null) {
            callableFuture.call(new CommandResult(CommandResult.STATUS_FAILED, m));
        }
    }

    public static class ReadCharacteristicOperation extends CharacteristicOperation {

        public ReadCharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(device, serviceUUID, characteristicUUID, callBack, callableFuture, context);
        }

        @Override
        protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (!gatt.readCharacteristic(characteristic) && callableFuture!=null) {
                callableFuture.call(cmdResFail());
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = characteristic.getValue();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                callBack.onRead(value);
                if (callableFuture!=null) {
                    callableFuture.call(cmdResSuccessValue(value));
                }
                return;
            }

//          TODO: handle BluetoothGatt.GATT_WRITE_NOT_PERMITTED and others
            if (callableFuture!=null) {
                callableFuture.call(cmdResFailStatusAndValue(String.valueOf(status), value));
            }
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
            if (!gatt.writeCharacteristic(characteristic) && callableFuture!=null) {
                callableFuture.call(cmdResFail());
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            callBack.onWrite(status);
            if (callableFuture!=null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    callableFuture.call(cmdResSuccess());
                } else {
//                TODO: handle BluetoothGatt.GATT_WRITE_NOT_PERMITTED and others
                    callableFuture.call(cmdResFailStatusAndValue(String.valueOf(status), value));
                }
            }
        }
    }

    public abstract static class CharacteristicOperation extends CommandResultReporter {
        protected GattCharacteristicCallBack callBack;
        protected SimpleCallableFuture<CommandResult> callableFuture;

        public CharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(serviceUUID, characteristicUUID, device, context);
            this.callBack = callBack;
            this.callableFuture = callableFuture;
        }

        public void call(BluetoothGatt gatt) {
            BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    request(gatt, characteristic);
//                    post delayed handler for operations without response
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!callableFuture.isGetDone()) {
                                callableFuture.call(cmdResSuccessStatus(context.getString(R.string.status_timeout)));
                            }
                        }
                    }, BluetoothServer.COMMAND_SCAN_DELAY);
                    return;
                }
            }

            if (callableFuture!=null) {
                callableFuture.call(cmdResFailStatus(context.getString(R.string.status_json_not_found)));
            }
        }

        abstract protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        abstract public void onResult(BluetoothGattCharacteristic characteristic, int status);
    }

    public interface ServicesDiscoveredCallback {
        void call(BluetoothGatt gatt);
    }

    public interface CharacteristicsDiscoveringCallback {
        void call(BluetoothGatt gatt);
    }

    public interface DisconnecListener {
        void onDisconnect();
    }

    abstract public static class NotificaitonSubscription extends CommandResultReporter{
        private static final String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
        private SimpleCallableFuture<CommandResult> future;
        private boolean isOn;
        public boolean isOn() {
            return isOn;
        }

        protected NotificaitonSubscription(String serviceUUID, String characteristicUUID, String device, Context context,
                                           boolean isOn, SimpleCallableFuture<CommandResult> future) {
            super(serviceUUID, characteristicUUID, device, context);
            this.isOn = isOn;
            this.future = future;
        }

//        should succeed only if service/characteristic found and all return results are true
        public void subscribe(BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));

                if (characteristic != null) {

                    if (gatt.setCharacteristicNotification(characteristic, isOn)) {
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DESCRIPTOR_UUID));

                        if (descriptor!=null) {
                            if (!descriptor.setValue(isOn ?
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                                future.call(cmdResFailStatus("failed set descriptor value"));
                                return;
                            }

                            if (gatt.writeDescriptor(descriptor)) {
                                future.call(cmdResSuccess());
                                return;
                            }
                        }
                        future.call(cmdResFailStatus("failed set characteristic notification"));
                    }
                }
            }

            future.call(cmdResNotFound());
        }

        abstract public void onNotification(byte[] value);
    }
}
