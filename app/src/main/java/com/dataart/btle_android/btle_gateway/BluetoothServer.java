package com.dataart.btle_android.btle_gateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.gatt.callbacks.DeviceConnection;
import com.dataart.btle_android.btle_gateway.gatt.callbacks.InteractiveGattCallback;
import com.dataart.btle_android.btle_gateway.gatt.callbacks.StatusJson;
import com.google.gson.Gson;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

/**
 * Created by alrybakov
 */
public class BluetoothServer extends BluetoothGattCallback {

    public static final int COMMAND_SCAN_DELAY = 10 * 1000; // 10 sec

    public static final String TAG = "BTLE Device Hive";

    private Context context;
    private BluetoothAdapter bluetoothAdapter = null;
//    Stores list of currently connected devices with adress, gatt and callback
    private Map<String, DeviceConnection> activeConnections = new HashMap<>();

    private ArrayList<LeScanResult> deviceList = new ArrayList<>();
    private DiscoveredDeviceListener discoveredDeviceListener;

    public BluetoothServer(Context context) {
        this.context = context;
    }

    private BluetoothAdapter bluetoothAdapter() {
        if (bluetoothAdapter == null) {
            BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        return bluetoothAdapter;
    }

    public ArrayList<BTLEDevice> getDiscoveredDevices() {
        final ArrayList<BTLEDevice> devices = new ArrayList<>(deviceList.size());
        for (LeScanResult result : deviceList) {
            String name = "Unknown name";
            String address = "Unknown address";
            if (!TextUtils.isEmpty(result.getDevice().getName())) {
                name = result.getDevice().getName();
            }
            if (!TextUtils.isEmpty(result.getDevice().getAddress())) {
                address = result.getDevice().getAddress();
            }
            final BTLEDevice device = new BTLEDevice(name, address);
            devices.add(device);
        }
        return devices;
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            onDeviceFound(device, rssi, scanRecord);
        }
    };

    private LeScanResult onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            Skip if already found
        //noinspection SuspiciousMethodCalls
        for(LeScanResult leScanResult:deviceList){
            if(leScanResult.getDevice().getAddress().equals(device.getAddress())) {
                return leScanResult;
            }
        }

        LeScanResult leScanResult = new LeScanResult(device, rssi, scanRecord);
        addDevice(leScanResult);
        if (discoveredDeviceListener != null) {
            discoveredDeviceListener.onDiscoveredDevice(device);
        }

        return leScanResult;
    }

    public void scanStart() {
        Timber.d("BLE scan started...");
        bluetoothAdapter().startLeScan(leScanCallback);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
//              "Never scan on a loop, and set a time limit on your scan. " - https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
                bluetoothAdapter().stopLeScan(leScanCallback);
                Timber.d("BLE scan stopped on timeout " + BluetoothServer.COMMAND_SCAN_DELAY / 1000 + " sec");
            }
        }, BluetoothServer.COMMAND_SCAN_DELAY);
    }

    public void scanStop() {
        Log.d(TAG, "Stop BLE Scan");
        bluetoothAdapter().stopLeScan(leScanCallback);
    }

    protected void addDevice(final LeScanResult device) {
        deviceList.add(device);
        Log.d(TAG, "BTdeviceName " + device.getDevice().getName());
        Log.d(TAG, "BTdeviceAdress " + device.getDevice().getAddress());
        Log.d(TAG, "scanRecord " + Arrays.toString(device.getScanRecord()));
    }

    private LeScanResult getResultByUDID(final String mac) {
        LeScanResult result = null;
        for (LeScanResult device : deviceList) {
            if (device.getDevice().getAddress().equals(mac)) {
                result = device;
                break;
            }
        }
        return result;
    }

    private interface DeviceOperation {
        void call(BluetoothDevice device);
        void fail(String message);
    }

//    Will seek among discovered but not connected devices
    private void applyForDevice(String address, DeviceOperation operation) {
        LeScanResult result = getResultByUDID(address);
        if (result != null) {
            operation.call(result.getDevice());
        } else {
            String message = String.format(context.getString(R.string.device_not_found), address);
            Timber.d(message);
            operation.fail(message);
        }
    }

    private interface ConnectionOperation {
        void call(DeviceConnection connection);
        void fail(String message);
    }

    private class ScanCallbacks {
        private String address;
        private ConnectionOperation operation;
        private BluetoothAdapter.LeScanCallback localCallback;

        public ScanCallbacks(final String address, final ConnectionOperation operation) {
            this.address = address;
            this.operation = operation;
            this.localCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

                    if (bluetoothDevice.getAddress().equals(address)) {
                        stop();
                        onDeviceFound(bluetoothDevice, i, bytes);

                        DeviceConnection connection = connectAndSave(address, bluetoothDevice);
                        operation.call(connection);
                    };
                }
            };
        }

        public void start(){
            Timber.d("no device or connection - scanning for device");
//              scan for device, add it to discovered devices, connect and call operation

            bluetoothAdapter().startLeScan(localCallback);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
//              "Never scan on a loop, and set a time limit on your scan. " - https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
                    Timber.d("on timeout");
                    stop();
                    operation.fail("\"s\":\"ok\"");
                }
            }, BluetoothServer.COMMAND_SCAN_DELAY);
        }

        public void stop(){
            Timber.d("stop");
            bluetoothAdapter().stopLeScan(localCallback);
        }

    }

    private void applyForConnectionOrScan(final String address, final ConnectionOperation operation) {
        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                operation.call(connection);
            }

            @Override
            public void fail(String message) {
                Timber.d("fail - scanning for device");
                new ScanCallbacks(address, operation).start();
            }
        }, true);
    }

//    Will seek among connected devices
    private void applyForConnection(String address, ConnectionOperation operation) {
        DeviceConnection connection = activeConnections.get(address);
        if (connection != null) {
            operation.call(connection);
        } else {
            String m = String.format(context.getString(R.string.connection_not_established), address);
            Timber.d(m);
            operation.fail(m);
        }
    }

    private void applyForConnection(final String address, final ConnectionOperation operation, final boolean autoconnect) {
        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                Timber.d("connection already established");
                operation.call(connection);
            }

            @Override
            public void fail(String message) {
                if (autoconnect) {
                    Timber.d("use autoconnect");
                    applyForDevice(address, new DeviceOperation() {
                        @Override
                        public void call(BluetoothDevice device) {
                            Timber.d("device found. connecting");
                            DeviceConnection connection = connectAndSave(address, device);
                            Timber.d("calling operation on new connection");
                            operation.call(connection);
                        }

                        @Override
                        public void fail(String message) {
                            Timber.d("device " + address + " not found - try to scan once more");
                            operation.fail(message);
                        }
                    });
                } else {
                    Timber.d("failed without autoconnect");
                    operation.fail(message);
                }
            }
        });
    }

    private DeviceConnection connectAndSave(String address, BluetoothDevice device) {
        return connectAndSave(address, device, null, null);
    }

    private DeviceConnection connectAndSave(String address, BluetoothDevice device, InteractiveGattCallback.DisconnecListener disconnecListener, SimpleCallableFuture<CommandResult> future) {
        final InteractiveGattCallback callback = new InteractiveGattCallback(address, future, context, disconnecListener);
        BluetoothGatt gatt = device.connectGatt(context, false, callback);
        DeviceConnection connection = new DeviceConnection(address, gatt, callback);
        activeConnections.put(address, connection);
        return connection;
    }

    public SimpleCallableFuture<CommandResult> gattConnect(final String address, final InteractiveGattCallback.DisconnecListener disconnecListener) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        applyForDevice(address, new DeviceOperation() {
            @Override
            public void call(BluetoothDevice device) {
                Timber.d("connecting to " + address);

//              We can store mutliple connections - and each should have it's own callback
                final InteractiveGattCallback callback = connectAndSave(address, device, disconnecListener, future).getCallback();

//              Send connection failed result after timeout
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!callback.isConnectionStateChanged()) {
                            future.call(new CommandResult(CommandResult.STATUS_FAILED, context.getString(R.string.connection_timeout)));
                        }
                    }
                }, BluetoothServer.COMMAND_SCAN_DELAY);

                Timber.d("connection added. connections now: " + activeConnections.size());
            }

            @Override
            public void fail(String message) {
                future.call(new CommandResult(CommandResult.STATUS_FAILED, message));
            }
        });

        return future;
    }

    public SimpleCallableFuture<CommandResult> gattDisconnect(final String address) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                Timber.d("disconnecting from " + address);
                connection.getGatt().disconnect();
                activeConnections.remove(address);
                Timber.d("disconnected. connections left: " + activeConnections.size());
                future.call(new CommandResult(CommandResult.STATUS_COMLETED, context.getString(R.string.status_ok)));
            }

            @Override
            public void fail(String message) {
                future.call(new CommandResult(CommandResult.STATUS_FAILED, message));
            }
        });

        return future;
    }

    public void gattCharacteristics(final String address, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final ArrayList<BTLECharacteristic> allCharacteristics = new ArrayList<>();

        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().setCharacteristicsDiscoveringCallback(new InteractiveGattCallback.CharacteristicsDiscoveringCallback() {
                    @Override
                    public void call(BluetoothGatt gatt) {
                        Timber.d("ServicesDiscoveredCallback.call() - installed from .gattPrimary()");

                        for (BluetoothGattService service : gatt.getServices()) {
                            final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {
                                BTLECharacteristic btleCharacteristic = new BTLECharacteristic(address, service.getUuid().toString(), characteristic.getUuid().toString());
                                allCharacteristics.add(btleCharacteristic);
                            }
                        }
                        gattCharacteristicCallBack.onCharacteristics(allCharacteristics);
                    }
                });
            }

            @Override
            public void fail(String message) {
                Timber.d("fail");
            }
        }, true);
    }

    public void gattPrimary(final String address, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final List<ParcelUuid> parcelUuidList = new ArrayList<>();

        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().setServicesDiscoveredCallback(new InteractiveGattCallback.ServicesDiscoveredCallback() {
                    @Override
                    public void call(BluetoothGatt gatt) {
                        Timber.d("ServicesDiscoveredCallback.call() - installed from .gattPrimary()");

                        final List<BluetoothGattService> services = gatt.getServices();
                        for (BluetoothGattService service : services) {
                            parcelUuidList.add(new ParcelUuid(service.getUuid()));
                        }
                        gattCharacteristicCallBack.onServices(parcelUuidList);
                    }
                });
            }

            @Override
            public void fail(String message) {
                Timber.d("fail");
            }
        }, true);
    }

    public SimpleCallableFuture<CommandResult> gattRead(Context context, final String address, final String serviceUUID, final String characteristicUUID,
                         final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().readCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack, future);
            }

            @Override
            public void fail(String message) {
                future.call(new CommandResult(CommandResult.STATUS_FAILED, message));
            }
        });

        return future;
    }

    public SimpleCallableFuture<CommandResult> gattWrite(Context context, final String address, final String serviceUUID, final String characteristicUUID,
                          final byte[] value, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().writeCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack, value, future);
            }

            @Override
            public void fail(String message) {
                future.call(new CommandResult(CommandResult.STATUS_FAILED, message));
            }
        });

        return future;
    }

    public SimpleCallableFuture<CommandResult> gattNotifications(final Context context, final String address, final String serviceUUID, final String characteristicUUID,
                                  final boolean isOn, final GattCharacteristicCallBack gattCharachteristicCallBack) {

        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();
        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().setNotificaitonSubscription(new InteractiveGattCallback.NotificaitonSubscription(serviceUUID, characteristicUUID, address, context, isOn, future) {
                    @Override
                    public void onNotification(byte[] value) {
                        Timber.d("onNotification"+value);
                        gattCharachteristicCallBack.onRead(value);
                    }
                });
            }

            @Override
            public void fail(String message) {
                Timber.d(message);
            }
        }, true);

        return future;
    }

    public interface DiscoveredDeviceListener {
        void onDiscoveredDevice(BluetoothDevice device);
    }

    private class LeScanResult {

        private BluetoothDevice mDevice;
        private int mRssi;
        private byte[] mScanRecord;

        public LeScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
            mDevice = device;
            mRssi = rssi;
            mScanRecord = scanRecord;
        }

        public BluetoothDevice getDevice() {
            return mDevice;
        }

        public int getRssi() {
            return mRssi;
        }

        public byte[] getScanRecord() {
            return mScanRecord;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (o instanceof BluetoothDevice){
                 return this.mDevice.getAddress().equals(((BluetoothDevice)o).getAddress());
            }

//            devices equals by address
            if (o instanceof LeScanResult){
                if (this.mDevice == null || ((LeScanResult) o).mDevice == null)
                    return false;

                return this.mDevice.getAddress().equals(((LeScanResult) o).mDevice.getAddress());
            }

            return false;
        }
    }

}
