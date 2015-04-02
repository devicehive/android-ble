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

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.future.SimpleCallableFuture;
import com.dataart.btle_android.btle_gateway.gatt.callbacks.DeviceConnection;
import com.dataart.btle_android.btle_gateway.gatt.callbacks.InteractiveGattCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * Created by alrybakov
 */
public class BluetoothServer extends BluetoothGattCallback {

    public static final int COMMAND_SCAN_DELAY = 10 * 1000; // 10 sec

    public static final String TAG = "BTLE Device Hive";

    private Context context;
    private BluetoothAdapter bluetoothAdapter = null;
//    stores list of currently connected devices storing binding between adress, gatt and callback
    private Map<String, DeviceConnection> activeConnections = new HashMap<String, DeviceConnection>();

    private ArrayList<LeScanResult> deviceList = new ArrayList<LeScanResult>();
    private DiscoveredDeviceListener discoveredDeviceListener;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

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
        final ArrayList<BTLEDevice> devices = new ArrayList<BTLEDevice>(deviceList.size());
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

//    FIXME:  unused
//    public void setDiscoveredDeviceListener(final DiscoveredDeviceListener discoveredDeviceListener) {
//        this.discoveredDeviceListener = discoveredDeviceListener;
//    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            skip if already found
            for (LeScanResult result : deviceList) {
                if (result.getDevice().getAddress().equals(device.getAddress())) {
                    return;
                }
            }

            addDevice(new LeScanResult(device, rssi, scanRecord));
            if (discoveredDeviceListener != null) {
                discoveredDeviceListener.onDiscoveredDevice(device);
            }
        }
    };

    public void scanStart(final Context context) {
        Timber.d("BLE scan started...");
        bluetoothAdapter().startLeScan(leScanCallback);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
//              "Never scan on a loop, and set a time limit on your scan. " - https://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find
                bluetoothAdapter().stopLeScan(leScanCallback);
                Timber.d("BLE scan stopped on timeout "+BluetoothServer.COMMAND_SCAN_DELAY /1000+" sec");
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
    }

//    will seek among discovered but not connected devices
    private void applyForDevice(String address, DeviceOperation operation) {
        LeScanResult result = getResultByUDID(address);
        if (result != null) {
            operation.call(result.getDevice());
        } else {
            Timber.d("device "+address+" not found in discovered devices");
        }
    }

    private interface ConnectionOperation {
        void call(DeviceConnection connection);
    }
//    will seek among connected devices
    private void applyForConnection(String address, ConnectionOperation operation) {
        DeviceConnection connection = activeConnections.get(address);
        if (connection != null) {
            operation.call(connection);
        } else {
            Timber.d("connection to "+address+" not established");
        }
    }

    public SimpleCallableFuture<CommandResult> gattConnect(final String address) {
//            , final Command.UpdateCommandStatusCallback commandStatusCallback) {

        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<CommandResult>();
        applyForDevice(address, new DeviceOperation() {
            @Override
            public void call(BluetoothDevice device) {
                Timber.d("connecting to " + address);
//                commandStatusCallback.setTag(address);
//                we need separate callbacks for different connections - and we can keep a lot of connections
                InteractiveGattCallback callback = new InteractiveGattCallback(future);
                BluetoothGatt gatt = device.connectGatt(context, false, callback);
                activeConnections.put(address, new DeviceConnection(address, gatt, callback));
                Timber.d("connection added. connections now: " + activeConnections.size());
            }
        });

        return future;
//        return new CommandResult(CommandResult.STATUS_WAITING, "Waiting for connection");
    }

    public void gattDisconnect(final String address) {
        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                Timber.d("disconnecting from " + address);
                connection.getGatt().disconnect();
                activeConnections.remove(address);
                Timber.d("disconnected. connections left: "+activeConnections.size());
//                return new CommandResult(CommandResult.STATUS_COMLETED, "Ok");
            }
        });
    }

    public void gattCharacteristics(final String mac, final Context context, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final LeScanResult result = getResultByUDID(mac);
        final ArrayList<BTLECharacteristic> allCharacteristics = new ArrayList<BTLECharacteristic>();
        if (result != null) {
            final BluetoothGatt gatt = result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {

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
                    for (BluetoothGattService service : gatt.getServices()) {
                        final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            final BTLECharacteristic btleCharacteristic = new BTLECharacteristic(mac,
                                    service.getUuid().toString(), characteristic.getUuid().toString());
                            allCharacteristics.add(btleCharacteristic);
                        }
                    }
                    gattCharacteristicCallBack.onCharacteristics(allCharacteristics);
                    gatt.disconnect();
                }
            });
        }
    }

    public void gattPrimary(final String mac, final Context context, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final LeScanResult result = getResultByUDID(mac);
        final List<ParcelUuid> parcelUuidList = new ArrayList<ParcelUuid>();
        if (result != null) {
            final BluetoothGatt gatt = result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        gatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        super.onServicesDiscovered(gatt, status);
                        final List<BluetoothGattService> services = gatt.getServices();
                        for (BluetoothGattService service : services) {
                            parcelUuidList.add(new ParcelUuid(service.getUuid()));
                        }
                        gattCharacteristicCallBack.onServices(parcelUuidList);
                        gatt.disconnect();
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                }
            });
        }
    }

    public void gattRead(Context context, final String address, final String serviceUUID, final String characteristicUUID,
                         final GattCharacteristicCallBack gattCharacteristicCallBack) {

        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().readCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack);
            }
        });
    }

    public void gattWrite(Context context, final String address, final String serviceUUID, final String characteristicUUID,
                          final byte[] value, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        applyForConnection(address, new ConnectionOperation() {
            @Override
            public void call(DeviceConnection connection) {
                connection.getCallback().writeCharacteristic(serviceUUID, characteristicUUID, gattCharacteristicCallBack, value);
            }
        });
    }

    public void gattNotifications(Context context, final String deviceUUID, final String serviceUUID, final String characteristicUUID,
                                  final boolean isOn, final GattCharacteristicCallBack gattCharachteristicCallBack) {
        final LeScanResult result = getResultByUDID(deviceUUID);
        if (result != null) {
            BluetoothGatt gatt = result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    gattCharachteristicCallBack.onRead(characteristic.getValue());
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
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
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));

                        gatt.setCharacteristicNotification(characteristic, isOn);

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(isOn ?
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    gattCharachteristicCallBack.onRead(characteristic.getValue());
                }
            });
        }
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
    }

}
