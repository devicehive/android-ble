package com.dataart.btle_android.btle_gateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.future.CmdResFuture;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.gatt_callbacks.CmdResult;
import com.dataart.btle_android.btle_gateway.gatt_callbacks.DeviceConnection;
import com.dataart.btle_android.btle_gateway.gatt_callbacks.InteractiveGattCallback;
import com.dataart.btle_android.helpers.BleHelpersFactory;
import com.dataart.btle_android.helpers.ble.base.BleScanner;

import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private BleScanner scanner;
    private BleScanner.ScanCallback scanCallback = this::onDeviceFound;

    public BluetoothServer(Context context) {
        this.context = context;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
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

    private LeScanResult onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            Skip if already found
        //noinspection SuspiciousMethodCalls
        for (LeScanResult leScanResult : deviceList) {
            if (leScanResult.getDevice().getAddress().equals(device.getAddress())) {
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
        Timber.d("BLE startScan started...");

        if (scanner == null) {
            scanner = getScanner();
        }

        scanner.startScan();

        final Handler handler = new Handler();
        handler.postDelayed(() -> {

//              "Never startScan on a loop, and set a time limit on your startScan. " - https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
            scanner.stopScan();
            Timber.d("BLE startScan stopped on timeout " + BluetoothServer.COMMAND_SCAN_DELAY / 1000 + " sec");

        }, BluetoothServer.COMMAND_SCAN_DELAY);
    }

    private BleScanner getScanner() {
        if (scanner == null) {
            scanner = BleHelpersFactory.getScanner(scanCallback, getBluetoothAdapter());
        }

        return scanner;
    }

    public void scanStop() {
        Log.d(TAG, "Stop BLE Scan");
        scanner.stopScan();
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
                            connectAndSave(address, device, () -> {
                                Timber.d("calling operation on successfull connection");
                                applyForConnection(address, operation);
                            });
                        }

                        @Override
                        public void fail(String message) {
                            Timber.d("device " + address + " not found - try to startScan once more");
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

    private DeviceConnection connectAndSave(String address, BluetoothDevice device, InteractiveGattCallback.OnConnectedListener connectedListener) {
        return connectAndSave(address, device, null, null, connectedListener);
    }

    private DeviceConnection connectAndSave(String address, BluetoothDevice device, InteractiveGattCallback.DisconnectListener disconnectListener, SimpleCallableFuture<CommandResult> future) {
        return connectAndSave(address, device, disconnectListener, future, null);
    }

    private DeviceConnection connectAndSave(String address, BluetoothDevice device, InteractiveGattCallback.DisconnectListener disconnectListener, SimpleCallableFuture<CommandResult> future, InteractiveGattCallback.OnConnectedListener connectedListener) {
        final InteractiveGattCallback callback = new InteractiveGattCallback(address, future, context, disconnectListener, connectedListener);
        BluetoothGatt gatt = device.connectGatt(context, false, callback);
        DeviceConnection connection = new DeviceConnection(address, gatt, callback);
        activeConnections.put(address, connection);
        return connection;
    }

    public SimpleCallableFuture<CommandResult> gattConnect(final String address, final InteractiveGattCallback.DisconnectListener disconnectListener) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        applyForDevice(address, new DeviceOperation() {
            @Override
            public void call(BluetoothDevice device) {
                Timber.d("connecting to " + address);

//              We can store mutliple connections - and each should have it's own callback
                final InteractiveGattCallback callback = connectAndSave(address, device, disconnectListener, future).getCallback();

//              Send connection failed result after timeout
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!callback.isConnectionStateChanged()) {
                            future.call(CmdResult.failTimeoutReached());
                        }
                    }
                }, BluetoothServer.COMMAND_SCAN_DELAY);

                Timber.d("connection added. connections now: " + activeConnections.size());
            }

            @Override
            public void fail(String message) {
                future.call(CmdResult.failWithStatus(message));
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
                future.call(CmdResult.success());
            }

            @Override
            public void fail(String message) {
                future.call(CmdResult.failWithStatus(message));
            }
        });

        return future;
    }

    public void gattCharacteristics(final String address, final GattCharacteristicCallBack gattCharacteristicCallBack, final CmdResFuture future) {
        final ArrayList<BTLECharacteristic> allCharacteristics = new ArrayList<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(final DeviceConnection connection) {
                connection.getCallback().setCharacteristicsDiscoveringCallback(new InteractiveGattCallback.CharacteristicsDiscoveringCallback() {
                    @Override
                    public void call(BluetoothGatt gatt) {
                        synchronized (connection) {
                            Timber.d("CharacteristicsDiscoveredCallback.call()");

                            for (BluetoothGattService service : gatt.getServices()) {
                                final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                                for (BluetoothGattCharacteristic characteristic : characteristics) {
                                    BTLECharacteristic btleCharacteristic = new BTLECharacteristic(address, service.getUuid().toString(), characteristic.getUuid().toString());
                                    allCharacteristics.add(btleCharacteristic);
                                }
                            }
                            gattCharacteristicCallBack.onCharacteristics(allCharacteristics);
                        }
                    }
                });
            }

            @Override
            public void fail(String message) {
                Timber.d("fail");
                future.call(CmdResult.failWithStatus(message));
            }
        });
    }

    public void gattPrimary(final String address, final GattCharacteristicCallBack gattCharacteristicCallBack, final CmdResFuture future) {
        final List<ParcelUuid> parcelUuidList = new ArrayList<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().setServicesDiscoveredCallback(new InteractiveGattCallback.ServicesDiscoveredCallback() {
                    @Override
                    public void call(BluetoothGatt gatt) {
                        Timber.d("ServicesDiscoveredCallback.call()");

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
                future.call(CmdResult.failWithStatus(message));

                Timber.d("fail");
            }
        });
    }

    public SimpleCallableFuture<CommandResult> gattRead(final String address, final String serviceUUID, final String characteristicUUID,
                                                        final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().readCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack, future);
            }

            @Override
            public void fail(String message) {
                future.call(CmdResult.failWithStatus(message));
            }
        });

        return future;
    }

    public SimpleCallableFuture<CommandResult> gattWrite(final String address, final String serviceUUID, final String characteristicUUID,
                                                         final byte[] value, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().writeCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack, value, future);
            }

            @Override
            public void fail(String message) {
                future.call(CmdResult.failWithStatus(message));
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
                String sUUID = serviceUUID;
                if (sUUID.length() == 4) {
                    sUUID = connection.getCallback().getFullServiceUuid(sUUID);
                }
                String cUUID = characteristicUUID;
                if (cUUID.length() == 4) {
                    cUUID = connection.getCallback().getFullCharacteristicUuid(cUUID);
                }

                connection.getCallback().setNotificaitonSubscription(new InteractiveGattCallback.NotificaitonSubscription(sUUID, cUUID, address, context, isOn, future) {
                    @Override
                    public void onNotification(byte[] value) {
                        Timber.d("onNotification: 0x" + String.valueOf(Hex.encodeHex(value)));
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

    private interface DeviceOperation {
        void call(BluetoothDevice device);

        void fail(String message);
    }

    private interface ConnectionOperation {
        void call(DeviceConnection connection);

        void fail(String message);
    }

    public interface DiscoveredDeviceListener {
        void onDiscoveredDevice(BluetoothDevice device);
    }

    private class ScanCallbacks {
        private String address;
        private ConnectionOperation operation;
        private BleScanner.ScanCallback localCallback;
        private BleScanner scanner;

        public ScanCallbacks(final String address, final ConnectionOperation operation) {
            this.address = address;
            this.operation = operation;

            this.localCallback = new BleScanner.ScanCallback() {
                private boolean found = false;

                @Override
                public void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (!found && device.getAddress().equals(address)) {
                        found = true;
                        stop();
                        onDeviceFound(device, rssi, scanRecord);

                        connectAndSave(address, device, () -> {
                            Timber.d("on connected - calling operation");
                            applyForConnection(address, operation);
                        });
                    }
                }
            };
        }

        public void start() {
            Timber.d("no device or connection - scanning for device");
//              startScan for device, add it to discovered devices, connect and call operation

            scanner = BleHelpersFactory.getScanner(localCallback, getBluetoothAdapter());
            scanner.startScan();

            final Handler handler = new Handler();
            handler.postDelayed(() -> {
//              "Never startScan on a loop, and set a time limit on your startScan. " - https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
                Timber.d("on timeout");
                stop();
                operation.fail(BTLEApplication.getApplication().getString(R.string.status_notfound_timeout));
            }, BluetoothServer.COMMAND_SCAN_DELAY);
        }

        public void stop() {
            Timber.d("stop");
            scanner.stopScan();
        }

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

            if (o instanceof BluetoothDevice) {
                return this.mDevice.getAddress().equals(((BluetoothDevice) o).getAddress());
            }

//            devices equals by address
            if (o instanceof LeScanResult) {
                //noinspection SimplifiableIfStatement
                if (this.mDevice == null || ((LeScanResult) o).mDevice == null)
                    return false;

                return this.mDevice.getAddress().equals(((LeScanResult) o).mDevice.getAddress());
            }

            return false;
        }
    }

}
