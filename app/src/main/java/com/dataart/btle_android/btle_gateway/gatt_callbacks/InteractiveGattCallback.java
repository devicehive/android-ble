package com.dataart.btle_android.btle_gateway.gatt_callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.BluetoothServer;
import com.dataart.btle_android.btle_gateway.GattCharacteristicCallBack;

import org.apache.commons.codec.binary.Hex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private DisconnectListener disconnectListener;
    private OnConnectedListener connectedListener;
    private boolean connectionStateChanged = false;
//    Mapping from short to long service naming
    private Map<String, String> services = new HashMap<>();
    private Map<String, String> characteristics = new HashMap<>();

    public InteractiveGattCallback(String address, SimpleCallableFuture<CommandResult> future, Context context, DisconnectListener disconnectListener, OnConnectedListener connectedListener) {
        this.address = address;
        this.callableFuture = future;
        this.context = context;
        this.disconnectListener = disconnectListener;
        this.connectedListener = connectedListener;
    }

    public boolean isConnectionStateChanged() {
        return connectionStateChanged;
    }

    public void setServicesDiscoveredCallback(ServicesDiscoveredCallback servicesDiscoveredCallback) {
        this.servicesDiscoveredCallback = servicesDiscoveredCallback;
//        call callback is services are already discovered
        if(servicesDiscovered){
            servicesDiscoveredCallback.call(gatt);
        }
    }

    public void setCharacteristicsDiscoveringCallback(CharacteristicsDiscoveringCallback characteristicsDiscoveringCallback) {
        this.characteristicsDiscoveringCallback = characteristicsDiscoveringCallback;
//        call callback is services are already discovered
        if(servicesDiscovered){
            characteristicsDiscoveringCallback.call(gatt);
        }else {
            gatt.discoverServices();
        }
    }

    public void setNotificaitonSubscription(NotificaitonSubscription notificaitonSubscription) {
        this.notificaitonSubscription = notificaitonSubscription;
        if (servicesDiscovered) {
            this.notificaitonSubscription.subscribe(gatt);
            return;
        }

        gatt.discoverServices();
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        connectionStateChanged = true;

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Timber.d("isConnectionStateChanged. discovering services...");
            this.gatt = gatt;
            this.gatt.discoverServices();

            if (callableFuture != null && !callableFuture.isGetDone()) {
                callableFuture.call(CmdResult.success());
            }
            if (connectedListener != null) {
                connectedListener.call();
            }
        } else {
            String m = String.format(context.getString(R.string.connection_failed_result), status, newState);
            Timber.d(m);
            if (callableFuture != null && !callableFuture.isGetDone()) {
                callableFuture.call(CmdResult.failWithStatus(m));
            }
            if (disconnectListener != null) {
                disconnectListener.onDisconnect();
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            servicesDiscovered = true;

//        Put each service to mapping
            List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
            Timber.d("uuids map: {");
            for (BluetoothGattService bluetoothGattService : bluetoothGattServices) {
                String uuid = bluetoothGattService.getUuid().toString().toLowerCase();
//            F000XXXX-0451-4000-B000-000000000000
                String shortUuid = uuid.substring(4, 8).toLowerCase();
                Timber.d("srvUuid: " + shortUuid + "=" + uuid);
                services.put(shortUuid, uuid);

                List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = bluetoothGattService.getCharacteristics();
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattCharacteristics) {
                    String cUuid = bluetoothGattCharacteristic.getUuid().toString().toLowerCase();
//                F000AA12-0451-4000-B000-000000000000
                    String cShortUuid = cUuid.substring(4, 8).toLowerCase();
                    Timber.d("chUuid: " + shortUuid + "=" + cUuid);
                    characteristics.put(cShortUuid, cUuid);
                }
            }
            Timber.d("}");

            if (readOperation != null) {
                readOperation.call(gatt);
            }
            if (writeOperation != null) {
                writeOperation.call(gatt);
            }
            if (servicesDiscoveredCallback != null) {
                servicesDiscoveredCallback.call(gatt);
            }
            if (characteristicsDiscoveringCallback != null) {
                characteristicsDiscoveringCallback.call(gatt);
                characteristicsDiscoveringCallback = null;
            }
            if (notificaitonSubscription != null) {
                notificaitonSubscription.subscribe(gatt);
//                unsubscribe and don't listen for future notifications
                if (!notificaitonSubscription.isOn()) {
                    notificaitonSubscription = null;
                }
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Timber.d("onCharacteristicRead. notificaitonSubscription=" + (notificaitonSubscription != null ? 1 : 0));
        if (readOperation != null) {
            readOperation.onResult(characteristic, status);
//            Reset readOperation for future calls
            readOperation = null;
        }
        if (notificaitonSubscription != null) {
            notificaitonSubscription.onNotification(characteristic.getValue());
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Timber.d("onCharacteristicChanged. notificaitonSubscription=" + (notificaitonSubscription != null ? 1 : 0));
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
            callableFuture.call(
                    CmdResult.failWithStatus(m));
        }
    }

    public String getFullServiceUuid(String shortUuid){
        if (shortUuid==null){
            return null;
        }
        if (shortUuid.length()!=4){
            return shortUuid;
        }
        return services.get(shortUuid.toLowerCase());
    }

    public String getFullCharacteristicUuid(String shortUuid){
        if (shortUuid==null){
            return null;
        }
        if (shortUuid.length()!=4){
            return shortUuid;
        }
        return characteristics.get(shortUuid.toLowerCase());
    }

    public interface OnConnectedListener {
        void call();
    }

    public interface ServicesDiscoveredCallback {
        void call(BluetoothGatt gatt);
    }

    public interface CharacteristicsDiscoveringCallback {
        void call(BluetoothGatt gatt);
    }

    public interface DisconnectListener {
        void onDisconnect();
    }

    abstract public static class NotificaitonSubscription extends CmdResult {
        private static final String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
        private SimpleCallableFuture<CommandResult> future;
        private boolean isOn;

        protected NotificaitonSubscription(String serviceUUID, String characteristicUUID, String device, Context context,
                                           boolean isOn, SimpleCallableFuture<CommandResult> future) {
            super(serviceUUID, characteristicUUID, device, context);
            this.isOn = isOn;
            this.future = future;
        }

        public boolean isOn() {
            return isOn;
        }

        //        should succeed only if service/characteristic found and all return results are true
        public void subscribe(BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));

                if (characteristic != null) {

                    if (gatt.setCharacteristicNotification(characteristic, isOn)) {
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DESCRIPTOR_UUID));

                        if (descriptor != null) {
                            if (!descriptor.setValue(isOn ?
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                                future.call(cmdResFullFailStatus("failed set descriptor value"));
                                return;
                            }

                            if (gatt.writeDescriptor(descriptor)) {
                                future.call(sucessFull());
                                return;
                            }
                        }
                        future.call(cmdResFullFailStatus("failed set characteristic notification"));
                    }
                }
            }

            future.call(cmdResFullNotFound());
        }

        abstract public void onNotification(byte[] value);
    }

    public class ReadCharacteristicOperation extends CharacteristicOperation {

        public ReadCharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(device, serviceUUID, characteristicUUID, callBack, callableFuture, context);
        }

        @Override
        protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (!gatt.readCharacteristic(characteristic) && future != null) {
                future.call(cmdResFullFail());
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = characteristic.getValue();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                callBack.onRead(value);
                if (future != null) {
                    future.call(successFullWithVal(StatusJson.bytes2String(value)));
//                            "0x" + String.valueOf(Hex.encodeHex(value))));
                }
                return;
            }

//          TODO: handle BluetoothGatt.GATT_WRITE_NOT_PERMITTED and others
            if (future != null) {
                future.call(cmdResFullFailStatus(statusWithValue(status, value)));
            }
        }
    }

    public class WriteCharacteristicOperation extends CharacteristicOperation {
        private byte[] value;

        public WriteCharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, byte[] value, SimpleCallableFuture<CommandResult> callableFuture, Context context) {
            super(device, serviceUUID, characteristicUUID, callBack, callableFuture, context);
            this.value = value;
        }

        @Override
        protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            characteristic.setValue(value);
            if (!gatt.writeCharacteristic(characteristic) && future != null) {
                future.call(cmdResFullFail());
            }
        }

        @Override
        public void onResult(BluetoothGattCharacteristic characteristic, int status) {
            callBack.onWrite(status);
            if (future != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    future.call(sucessFull());
                } else {
                    int resId = R.string.status_fail;
                    switch (status) {
                        case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                            resId = R.string.status_fail_write_not_permitted;
                            break;
//                TODO: handle other BluetoothGatt error codes
                    }
                    future.call(withStatusAndVal(resId, StatusJson.bytes2String(value)));
                }
            }
        }
    }

    public abstract class CharacteristicOperation extends CmdResult {
        protected GattCharacteristicCallBack callBack;
        protected SimpleCallableFuture<CommandResult> future;

        public CharacteristicOperation(String device, String serviceUUID, String characteristicUUID, GattCharacteristicCallBack callBack, SimpleCallableFuture<CommandResult> future, Context context) {
//            first time we init operation with short or long uuid - no matter which format
            super(serviceUUID, characteristicUUID, device, context);
            this.callBack = callBack;
            this.future = future;
        }

        public void call(BluetoothGatt gatt) {
//            before execute call, we need convert uuids to long format because Android BLE Api understands only last
//            converstion can't be done in constructor because at that moment services might be not discovered
            if ((serviceUUID = getFullServiceUuid(serviceUUID)) == null) {
                future.call(failWithStatus(context.getString(R.string.status_service_uuid_nf)));
                return;
            }
            if ((characteristicUUID = getFullCharacteristicUuid(characteristicUUID)) == null) {
                future.call(failWithStatus(context.getString(R.string.status_char_uuid_nf)));
                return;
            }
//            call
            BluetoothGattService service;
            try {
                service = gatt.getService(UUID.fromString(serviceUUID));
            } catch (Exception e) {
                future.call(failWithStatus("gatt.getService(uuid) crashed: " + e.getMessage()));
                return;
            }

            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    request(gatt, characteristic);
//                    post delayed handler for operations without response
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!future.isGetDone()) {
                                future.call(successFullWithStatus(context.getString(R.string.status_timeout)));
                            }
                        }
                    }, BluetoothServer.COMMAND_SCAN_DELAY);
                    return;
                }
            }

            if (future != null) {
                future.call(cmdResFullFailStatus(context.getString(R.string.status_json_not_found)));
            }
        }

        protected String statusWithValue(int status, byte[] value) {
            return "{\"status\":\"" + String.valueOf(status) + "\",\"value\"=\"0x" + String.valueOf(Hex.encodeHex(value)) + "\"}";
        }

        abstract protected void request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        abstract public void onResult(BluetoothGattCharacteristic characteristic, int status);
    }
}
