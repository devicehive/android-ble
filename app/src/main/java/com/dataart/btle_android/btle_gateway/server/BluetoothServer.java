package com.dataart.btle_android.btle_gateway.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.GattCharacteristicCallBack;
import com.dataart.btle_android.btle_gateway.gatt_callbacks.InteractiveGattCallback;
import com.dataart.btle_android.btle_gateway.model.BTLECharacteristic;
import com.dataart.btle_android.btle_gateway.model.BTLEDevice;
import com.dataart.btle_android.btle_gateway.model.DeviceConnection;
import com.dataart.btle_android.helpers.BleHelpersFactory;
import com.dataart.btle_android.helpers.ble.base.BleScanner;

import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

/**
 * Created by alrybakov
 */
public class BluetoothServer extends BluetoothGattCallback {

    public static final int COMMAND_SCAN_DELAY = 10 * 1000; // 10 sec

    private final Context context;
    private BluetoothAdapter bluetoothAdapter = null;
    //    Stores list of currently connected devices with adress, gatt and callback
    private final Map<String, DeviceConnection> activeConnections = new HashMap<>();

    private final ArrayList<LeScanResult> deviceList = new ArrayList<>();

    private BleScanner scanner;
    private final BleScanner.ScanCallback scanCallback = this::onDeviceFound;

    public BluetoothServer(Context context) {
        this.context = context;
    }

    BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            assert bluetoothManager != null;
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
            if (Objects.equals(leScanResult.getDevice().getAddress(), device.getAddress())) {
                return leScanResult;
            }
        }

        LeScanResult leScanResult = new LeScanResult(device, rssi, scanRecord);
        addDevice(leScanResult);

        return leScanResult;
    }

    public void scanStart() {
        Timber.d("BLE startScan started...");

        if (scanner == null) {
            scanner = getScanner();
        }

        deviceList.clear();
        scanner.startScan();

        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(BluetoothServer.COMMAND_SCAN_DELAY);
                    //  https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
                    //  "Never startScan on a loop, and set a time limit on your startScan. "
                    scanner.stopScan();
                    Timber.d("BLE startScan stopped on timeout " + BluetoothServer.COMMAND_SCAN_DELAY / 1000 + " sec");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private BleScanner getScanner() {
        if (scanner == null) {
            scanner = BleHelpersFactory.getScanner(scanCallback, getBluetoothAdapter());
        }

        return scanner;
    }

    public void scanStop() {
        Timber.d("Stop BLE Scan");
        scanner.stopScan();
    }

    protected void addDevice(final LeScanResult device) {
        deviceList.add(device);
        Timber.d("BTdeviceName %s", device.getDevice().getName());
        Timber.d("BTdeviceAdress %s", device.getDevice().getAddress());
        Timber.d("scanRecord %s", Arrays.toString(device.getScanRecord()));
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
                new ScanCallbacks(BluetoothServer.this, address, operation).start();
            }
        }, true);
    }

    //    Will seek among connected devices
    void applyForConnection(String address, ConnectionOperation operation) {
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

    DeviceConnection connectAndSave(String address, BluetoothDevice device) {
        return connectAndSave(address, device, null, null);
    }

    @SuppressWarnings("UnusedReturnValue")
    DeviceConnection connectAndSave(String address, BluetoothDevice device, InteractiveGattCallback.OnConnectedListener connectedListener) {
        return connectAndSave(address, device, null, null, connectedListener);
    }

    DeviceConnection connectAndSave(String address, BluetoothDevice device, InteractiveGattCallback.DisconnectListener disconnectListener, InteractiveGattCallback.StatusListener statusListener) {
        return connectAndSave(address, device, disconnectListener, statusListener, null);
    }

    private DeviceConnection connectAndSave(String address, BluetoothDevice device, InteractiveGattCallback.DisconnectListener disconnectListener, InteractiveGattCallback.StatusListener statusListener, InteractiveGattCallback.OnConnectedListener connectedListener) {
        final InteractiveGattCallback callback = new InteractiveGattCallback(address, statusListener, context, disconnectListener, connectedListener);
        BluetoothGatt gatt = device.connectGatt(context, false, callback);
        DeviceConnection connection = new DeviceConnection(address, gatt, callback);
        activeConnections.put(address, connection);
        return connection;
    }

    public void gattConnect(final String address, final InteractiveGattCallback.DisconnectListener disconnectListener, final InteractiveGattCallback.StatusListener statusListener) {
        applyForDevice(address, new DeviceOperation() {
            @Override
            public void call(BluetoothDevice device) {
                Timber.d("connecting to %s", address);

//              We can store mutliple connections - and each should have it's own callback
                final InteractiveGattCallback callback = connectAndSave(address, device, disconnectListener, statusListener).getCallback();

//              Send connection failed result after timeout
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            sleep(BluetoothServer.COMMAND_SCAN_DELAY);
                            if (callback.isConnectionStateNotChanged()) {
                                statusListener.onStatus(false, context.getString(R.string.status_timeout));
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

                Timber.d("connection added. connections now: %s", activeConnections.size());
            }

            @Override
            public void fail(String message) {
                statusListener.onStatus(false, message);
            }
        });
    }

    public void gattDisconnect(final String address, final InteractiveGattCallback.StatusListener statusListener) {
        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                Timber.d("disconnecting from %s", address);
                connection.getGatt().disconnect();
                activeConnections.remove(address);
                Timber.d("disconnected. connections left: %s", activeConnections.size());
                statusListener.onStatus(true, "");
            }

            @Override
            public void fail(String message) {
                statusListener.onStatus(false, message);
            }
        });
    }

    public void gattCharacteristics(final String address, final GattCharacteristicCallBack gattCharacteristicCallBack, final InteractiveGattCallback.StatusListener statusListener) {
        final ArrayList<BTLECharacteristic> allCharacteristics = new ArrayList<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(final DeviceConnection connection) {
                connection.getCallback().setCharacteristicsDiscoveringCallback(gatt -> {
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
                });
            }

            @Override
            public void fail(String message) {
                Timber.d("fail");
                statusListener.onStatus(false, message);
            }
        });
    }

    public void gattPrimary(final String address, final GattCharacteristicCallBack gattCharacteristicCallBack, final InteractiveGattCallback.StatusListener statusListener) {
        final List<ParcelUuid> parcelUuidList = new ArrayList<>();

        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().setServicesDiscoveredCallback(gatt -> {
                    Timber.d("ServicesDiscoveredCallback.call()");

                    final List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        parcelUuidList.add(new ParcelUuid(service.getUuid()));
                    }
                    gattCharacteristicCallBack.onServices(parcelUuidList);
                });
            }

            @Override
            public void fail(String message) {
                statusListener.onStatus(false, message);

                Timber.d("fail");
            }
        });
    }

    public void gattRead(final String address, final String serviceUUID, final String characteristicUUID,
                                                        final GattCharacteristicCallBack gattCharacteristicCallBack,
                                                        final InteractiveGattCallback.StatusListener statusListener) {
        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().readCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack, statusListener);
            }

            @Override
            public void fail(String message) {
                statusListener.onStatus(false, message);
            }
        });
    }

    public void gattWrite(final String address, final String serviceUUID, final String characteristicUUID,
                                                         final byte[] value, final GattCharacteristicCallBack gattCharacteristicCallBack,
                                                         final InteractiveGattCallback.StatusListener statusListener) {
        applyForConnectionOrScan(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().writeCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack, value, statusListener);
            }

            @Override
            public void fail(String message) {
                statusListener.onStatus(false, message);
            }
        });
    }

    public void gattNotifications(final Context context, final String address, final String serviceUUID, final String characteristicUUID,
                                                                 final boolean isOn, final GattCharacteristicCallBack gattCharachteristicCallBack,
                                                                 final InteractiveGattCallback.StatusListener statusListener) {
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

                connection.getCallback().setNotificationSubscription(new InteractiveGattCallback.NotificationSubscription(sUUID, cUUID, address, context, isOn, statusListener) {
                    @Override
                    public void onNotification(byte[] value) {
                        Timber.d("onNotification: 0x%s", String.valueOf(Hex.encodeHex(value)));
                        gattCharachteristicCallBack.onRead(value);
                    }
                });
            }

            @Override
            public void fail(String message) {
                Timber.d(message);
            }
        }, true);
    }
}
