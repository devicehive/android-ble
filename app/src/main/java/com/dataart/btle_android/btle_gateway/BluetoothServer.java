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
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by alrybakov
 */
public class BluetoothServer extends BluetoothGattCallback {

    public static final int COMMAND_SCAN_DEALY = 10 * 1000; // 10 sec

    public static final String TAG = "BTLE Device Hive";

    private BluetoothAdapter bluetoothAdapter = null;
    private ArrayList<LeScanResult> deviceList;

    private DiscoveredDeviceListener discoveredDeviceListener;

    public BluetoothServer() {
        deviceList = new ArrayList<LeScanResult>();
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

    public void setDiscoveredDeviceListener(final DiscoveredDeviceListener discoveredDeviceListener) {
        this.discoveredDeviceListener = discoveredDeviceListener;
    }

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            addDevice(new LeScanResult(device, rssi, scanRecord));
            if (discoveredDeviceListener != null) {
                discoveredDeviceListener.onDiscoveredDevice(device);
            }
        }
    };

    public void scanStart(final Context context) {
        Log.d(TAG, "Start BLE Scan");
        if (bluetoothAdapter == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    public void scanStop() {
        Log.d(TAG, "Stop BLE Scan");
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    protected void addDevice(final LeScanResult device) {
        for (LeScanResult result : deviceList) {
            if (result.getDevice().getAddress().equals(device.getDevice().getAddress())) {
                return;
            }
        }
        deviceList.add(device);
        Log.d(TAG, "BTdeviceName " + device.getDevice().getName());
        Log.d(TAG, "BTdeviceAdress " + device.getDevice().getAddress());
        Log.d(TAG, "scanRecord " + device.getScanRecord().toString());
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


    public void gattRead(Context context, final String deviceUUID, final String serviceUUID, final String characteristicUUID,
                         final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final LeScanResult result = getResultByUDID(deviceUUID);
        if (result != null) {
            final BluetoothGatt gatt = result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    gattCharacteristicCallBack.onRead(characteristic.getValue());
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic, int status) {
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
                        final BluetoothGattCharacteristic characteristic
                                = service.getCharacteristic(UUID.fromString(characteristicUUID));
                        if (characteristic != null) {
                            gatt.readCharacteristic(characteristic);
                        }
                    }
                }
            });
        }
    }

    public void gattWrite(Context context, final String deviceUUID, final String serviceUUID, final String characteristicUUID,
                          final byte[] value, final GattCharacteristicCallBack gattCharacteristicCallBack) {
        final LeScanResult result = getResultByUDID(deviceUUID);
        final BluetoothGatt gatt = result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                gattCharacteristicCallBack.onRead(characteristic.getValue());
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                gattCharacteristicCallBack.onWrite(status);
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
                    if (characteristic != null) {
                        characteristic.setValue(value);
                        gatt.writeCharacteristic(characteristic);
                    }
                }
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
