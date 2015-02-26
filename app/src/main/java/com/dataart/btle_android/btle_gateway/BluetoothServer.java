package com.dataart.btle_android.btle_gateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by alrybakov
 */
public class BluetoothServer extends BluetoothGattCallback {

    public static final int COMMAND_SCAN_DEALY = 10 * 1000; // 10 sec

    private BluetoothAdapter bluetoothAdapter;

    BluetoothLeScanner scanner;
    ArrayList<ScanResult> deviceList;

   public BluetoothServer() {
       deviceList = new ArrayList<>();
   }


    protected void addDevice(ScanResult device) {

        for(ScanResult result : deviceList) {
            if(result.getDevice().getAddress().equals(device.getDevice().getAddress())) {
                return;
            }
        }

         {
            deviceList.add(device);
            Log.d("TAG","BTdeviceName " + device.getDevice().getName());
            Log.d("TAG","BTdeviceAdress " + device.getDevice().getAddress());
            Log.d("TAG", "scanRecord " + device.getScanRecord().toString());
        }
    }


    public ArrayList<BTLEDevice> getDiscoveredDevices() {

        ArrayList<BTLEDevice> devices = new ArrayList<BTLEDevice>();

        for(ScanResult result : deviceList) {
            String name = "Unkown name";
            String address = "Unkown address";

            if(result.getDevice().getName() != null) {
                name = result.getDevice().getName();
            }

            if(result.getDevice().getAddress() != null) {
                address = result.getDevice().getAddress();
            }



            BTLEDevice device = new BTLEDevice(name, address);
            devices.add(device);
        }

        return devices;

    }



   public void scanStart(Context context) {

        if(bluetoothAdapter == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);


            bluetoothAdapter = bluetoothManager.getAdapter();
            scanner = bluetoothAdapter.getBluetoothLeScanner();

        }

       scanner.startScan(new ScanCallback() {
           @Override
           public void onScanResult(int callbackType, ScanResult result) {

               super.onScanResult(callbackType, result);
               addDevice(result);
               Log.d("TAG","onScanResult" + result);
           }

           @Override
           public void onBatchScanResults(List<ScanResult> results) {
               super.onBatchScanResults(results);
           }

           @Override
           public void onScanFailed(int errorCode) {
               super.onScanFailed(errorCode);
               Log.d("TAG","scanFailed" + errorCode);
           }
       });
   }

   public void scanStop() {
       scanner.stopScan(new ScanCallback() {
           @Override
           public void onScanResult(int callbackType, ScanResult result) {
               super.onScanResult(callbackType, result);
           }


           @Override
           public void onScanFailed(int errorCode) {
               super.onScanFailed(errorCode);
           }
       });

       //TODO: send devce info

   }

   public List<ParcelUuid> gattPrimary(String mac) {

       List<ParcelUuid>  services = null;

       ScanResult result = getResultByUDID(mac);
       if(result != null) {
           services = result.getScanRecord().getServiceUuids();
       }
               //services = result.getScanRecord().getServiceData()



       return services;

   }

    private ScanResult getResultByUDID(String mac) {
        ScanResult result = null;

        for(ScanResult device : deviceList) {

            if(device.getDevice().getAddress().equals(mac)) {

                result = device;
                break;

            }
        }

        return result;
    }




    public void  gattCharacteristics(final String mac, Context context, final GattCharachteristicCallBack callBack) {
        ScanResult result = getResultByUDID(mac);

        final ArrayList<BTLECharacteristic> allCharacteristics = new ArrayList<>();



        if(result != null) {


            final BluetoothGatt gatt =  result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if(newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);

                    for (BluetoothGattService service : gatt.getServices()) {
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                        for (BluetoothGattCharacteristic characteristic : characteristics) {

                            BTLECharacteristic btlch = new BTLECharacteristic(mac,service.getUuid().toString(), characteristic.getUuid().toString());
                            allCharacteristics.add(btlch);
                        }

                    }

                    callBack.characteristicsList(allCharacteristics);

                    gatt.disconnect();
                }
            });




        }








    }





    public void gattRead(Context context, final String deviceUUID,final  String serviceUUID,final  String characteristicUUID, final GattCharachteristicCallBack gattCharachteristicCallBack) {
        ScanResult result = getResultByUDID(deviceUUID);
        if(result != null) {
            BluetoothGatt gatt = result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    gattCharachteristicCallBack.onRead(characteristic.getValue());
                    //
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

                    BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
                    if (service != null) {
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));

                        if (characteristic != null) {

                            gatt.readCharacteristic(characteristic);
                        }
                    }

                }
            });
        }



    }

    public void gattWrite(Context context, final String deviceUUID, final String serviceUUID, final String characteristicUUID,final byte[] value, final GattCharachteristicCallBack gattCharachteristicCallBack) {
        ScanResult result = getResultByUDID(deviceUUID);
        BluetoothGatt gatt =  result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                gattCharachteristicCallBack.onRead(characteristic.getValue());
                //
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                gattCharachteristicCallBack.onWrite(status);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
                if(service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));

                    if(characteristic != null) {

                        characteristic.setValue(value);
                        gatt.writeCharacteristic(characteristic);
                    }
                }

            }
        });
    }

}
