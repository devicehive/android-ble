package com.dataart.btle_android.btle_gateway;

import android.content.Context;
import android.os.Handler;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.Notification;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class BTLEGateway {

    public static final String COMMAND_SCAN_START = "scan/start";
    public static final String COMMAND_SCAN_STOP = "scan/stop";
    public static final String COMMAND_SCAN = "scan";
    public static final String COMMAND_GATT_PRIMARY = "gatt/primary";
    public static final String COMMAND_GATT_CHARACTERISTICS = "gatt/characteristics";
    public static final String COMMAND_GATT_READ = "gatt/read";
    public static final String COMMAND_GATT_WRITE = "gatt/write";

    BluetoothServer bluetoothServerGateway;


    public BTLEGateway( BluetoothServer bluetoothServer) {

        this.bluetoothServerGateway = bluetoothServer;
    }

    public void doCommand(Context context, final BTLEDeviceHive dh, Command command) {

        try {


            String name = command.getCommand();

            if (COMMAND_SCAN_START.equals(name)) {
                bluetoothServerGateway.scanStart(context);
            } else if (COMMAND_SCAN_STOP.equals(name)) {
                bluetoothServerGateway.scanStop();

                sendStopResult(dh);

            } else if (COMMAND_SCAN.equals(name)) {
                bluetoothServerGateway.scanStart(context);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        sendStopResult(dh);

                    }
                }, BluetoothServer.COMMAND_SCAN_DEALY);
            } else if (COMMAND_GATT_PRIMARY.equals(name)) {
                HashMap<String, Object> params = (HashMap<String, Object>) command.getParameters();

                String deviceName = (String) params.get("device");


                String json = new Gson().toJson(
                        bluetoothServerGateway.gattPrimary(deviceName)
                );

                Notification notification = new Notification("gatt/primary", json);
                dh.sendNotification(notification);

            } else if (COMMAND_GATT_CHARACTERISTICS.equals(name)) {
                HashMap<String, Object> params = (HashMap<String, Object>) command.getParameters();
                String deviceName = (String) params.get("device");

                bluetoothServerGateway.gattCharacteristics(deviceName, context, new GattCharachteristicCallBack() {
                    @Override
                    public void characteristicsList(ArrayList<BTLECharacteristic> characteristics) {
                        String json = new Gson().toJson(characteristics);

                        Notification notification = new Notification(COMMAND_GATT_CHARACTERISTICS, json);
                        dh.sendNotification(notification);
                    }

                    @Override
                    public void onRead(byte[] value) {

                    }
                });

            } else if (COMMAND_GATT_READ.equals(name)) {
                HashMap<String, Object> params = (HashMap<String, Object>) command.getParameters();
                String deviceUUID = (String) params.get("device");
                String serviceUUID = (String) params.get("serviceUUID");
                String characteristicUUID = (String) params.get("characteristicUUID");


                bluetoothServerGateway.gattRead(context, deviceUUID, serviceUUID, characteristicUUID, new GattCharachteristicCallBack() {
                    @Override
                    public void characteristicsList(ArrayList<BTLECharacteristic> characteristics) {

                    }

                    @Override
                    public void onRead(byte[] value) {
                        String sValue = Utils.printHexBinary(value);
                        String json = new Gson().toJson(sValue);
                        Notification notification = new Notification(COMMAND_GATT_CHARACTERISTICS, json);
                        dh.sendNotification(notification);
                    }
                });
            } else if (COMMAND_GATT_WRITE.equals(name)) {
                HashMap<String, Object> params = (HashMap<String, Object>) command.getParameters();
                String deviceUUID = (String) params.get("device");
                String serviceUUID = (String) params.get("serviceUUID");
                String characteristicUUID = (String) params.get("characteristicUUID");
                String sValue = (String) params.get("value");

                byte[] value = Utils.parseHexBinary(sValue);

                bluetoothServerGateway.gattWrite(context, deviceUUID, serviceUUID, characteristicUUID, value, new GattCharachteristicCallBack() {
                    @Override
                    public void characteristicsList(ArrayList<BTLECharacteristic> characteristics) {

                    }

                    @Override
                    public void onWrite(int state) {
                        String json = new Gson().toJson(state);
                        Notification notification = new Notification(COMMAND_GATT_WRITE, json);
                        dh.sendNotification(notification);
                    }
                });
            }

        }
        catch(Throwable thr) {
            thr.printStackTrace();;

            Notification notification = new Notification("CRASH", thr.getMessage());
            dh.sendNotification(notification);
        }

    }






    private void sendStopResult(BTLEDeviceHive dh) {
        ArrayList<BTLEDevice> devices = bluetoothServerGateway.getDiscoveredDevices();
        String json = new Gson().toJson(devices);

        HashMap<String, String> result = new HashMap<>();
        result.put("result", json);

        Notification notification = new Notification("discoveredDevices", result);
        dh.sendNotification(notification);
    }


}
