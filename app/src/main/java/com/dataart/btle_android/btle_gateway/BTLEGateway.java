package com.dataart.btle_android.btle_gateway;

import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.btle_android.R;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.dataart.btle_android.btle_gateway.gatt.callbacks.InteractiveGattCallback;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class BTLEGateway {

    private Context context;
    private BluetoothServer bluetoothServerGateway;

    public BTLEGateway(Context context, BluetoothServer bluetoothServer) {
        this.context = context;
        this.bluetoothServerGateway = bluetoothServer;
    }

    public SimpleCallableFuture<CommandResult> doCommand(final Context context, final BTLEDeviceHive dh, final Command command) {
        try {
            Timber.d("doCommand");
            final String name = command.getCommand();
            final LeCommand leCommand = LeCommand.fromName(name);

            @SuppressWarnings("unchecked")
            final HashMap<String, Object> params = (HashMap<String, Object>) command.getParameters();

            final String address = (params != null) ? (String) params.get("device") : null;
            final String serviceUUID = (params != null) ? (String) params.get("serviceUUID") : null;
            final String characteristicUUID = (params != null) ? (String) params.get("characteristicUUID") : null;

            Timber.d("switch");
            switch (leCommand) {
                case SCAN_START:
                    bluetoothServerGateway.scanStart(context);
                    break;
                case SCAN_STOP:
                    bluetoothServerGateway.scanStop();
                    sendStopResult(dh);
                    break;
                case SCAN:
                    return scanAndReturnResults(dh);

                case GATT_CONNECT:
                    Timber.d("connecting to " + address);
                    return bluetoothServerGateway.gattConnect(address, new InteractiveGattCallback.DisconnecListener() {

                        @Override
                        public void onDisconnect() {
                            final String json = new Gson().toJson(String.format(context.getString(R.string.is_disconnected), address));
                            sendNotification(dh, leCommand, json);
                        }
                    });

                case GATT_DISCONNECT:
                    Timber.d("disconnecting from" + address);
                    return bluetoothServerGateway.gattDisconnect(address);

                case GATT_PRIMARY:

                    final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();
                    bluetoothServerGateway.gattPrimary(address, new GattCharacteristicCallBack() {
                        @Override
                        public void onServices(List<ParcelUuid> uuidList) {
                            final String json = new Gson().toJson(uuidList);
                            future.call(cmdResSuccessWithJson(json));
                            sendNotification(dh, leCommand, json);
                            future.call(cmdResSuccessWithJson(json));
                        }
                    });
                    return future;
                case GATT_CHARACTERISTICS:
                    bluetoothServerGateway.gattCharacteristics(address, context, new GattCharacteristicCallBack() {
                        @Override
                        public void onCharacteristics(ArrayList<BTLECharacteristic> characteristics) {
                            final String json = new Gson().toJson(characteristics);
                            sendNotification(dh, leCommand, json);
                        }
                    });
                    break;
                case GATT_READ:
                    return bluetoothServerGateway.gattRead(context, address, serviceUUID, characteristicUUID, new GattCharacteristicCallBack() {
                        @Override
                        public void onRead(byte[] value) {
                            final String sValue = Utils.printHexBinary(value);
                            final String json = new Gson().toJson(sValue);
                            sendNotification(dh, leCommand, json);
                        }
                    });

                case GATT_WRITE:
                    final String sValue = (String) (params!=null ? params.get("value") : null);
                    final byte[] value = Utils.parseHexBinary(sValue);
                    return bluetoothServerGateway.gattWrite(context, address, serviceUUID, characteristicUUID, value, new GattCharacteristicCallBack() {
                        @Override
                        public void onWrite(int state) {
                            final String json = new Gson().toJson(state);
                            sendNotification(dh, leCommand, json);
                        }
                    });

                case GATT_NOTIFICATION:
                    bluetoothServerGateway.gattNotifications(context, address, serviceUUID, characteristicUUID, true, new GattCharacteristicCallBack() {

                        @Override
                        public void onRead(byte[] value) {
                            final String sValue = Utils.printHexBinary(value);
                            final String json = new Gson().toJson(sValue);
                            sendNotification(dh, leCommand, json);
                        }
                    });
                    break;
                case GATT_NOTIFICATION_STOP:
                    bluetoothServerGateway.gattNotifications(context, address, serviceUUID, characteristicUUID, false, new GattCharacteristicCallBack() {

                        @Override
                        public void onRead(byte[] value) {
                            final String sValue = Utils.printHexBinary(value);
                            final String json = new Gson().toJson(sValue);
                            sendNotification(dh, leCommand, json);
                        }
                    });
                    break;
                case UNKNOWN:
                default:
                    new SimpleCallableFuture<>(new CommandResult(CommandResult.STATUS_FAILED, context.getString(R.string.unknown_command)));
            }
        } catch (Exception e) {
            Timber.e("error:"+e.toString());
//            Log.e("TAG", "Error during handling" + e.toString());
            final Notification notification = new Notification("Error", e.toString());
            dh.sendNotification(notification);
            return new SimpleCallableFuture<>(new CommandResult(CommandResult.STATUS_FAILED, "Error: \""+e.toString()+"\""));
        }

        Timber.d("default status ok");
        return new SimpleCallableFuture<>(cmdResSuccess());
    }

    private class Res {
        public String getStatus() {
            return status;
        }

        private String status;

        private Res(String status) {
            this.status = status;
        }
    }

    private SimpleCallableFuture<CommandResult> scanAndReturnResults(final BTLEDeviceHive dh) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        bluetoothServerGateway.scanStart(context);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendStopResult(dh, future);
            }
        }, BluetoothServer.COMMAND_SCAN_DELAY);

        return future;
    }

    private void sendNotification(final BTLEDeviceHive dh, final LeCommand leCommand, final String json) {
        final Notification notification = new Notification(leCommand.getCommand(), json);
        dh.sendNotification(notification);
    }

    private void sendStopResult(BTLEDeviceHive dh) {
        sendStopResult(dh, null);
    }

    private void sendStopResult(BTLEDeviceHive dh, SimpleCallableFuture<CommandResult> future) {
        final ArrayList<BTLEDevice> devices = bluetoothServerGateway.getDiscoveredDevices();
        final String json = new Gson().toJson(devices);

        final HashMap<String, String> result = new HashMap<>();
        result.put("result", json);

//        notify calling code about result
        if (future!=null) {
            future.call(new CommandResult(CommandResult.STATUS_COMLETED, json));
        }

        final Notification notification = new Notification("discoveredDevices", result);
        dh.sendNotification(notification);
    }

    public enum LeCommand {
        SCAN_START("scan/start"),
        SCAN_STOP("scan/stop"),
        SCAN("scan"),
        GATT_PRIMARY("gatt/primary"),
        GATT_CHARACTERISTICS("gatt/characteristics"),
        GATT_READ("gatt/read"),
        GATT_WRITE("gatt/write"),
        GATT_NOTIFICATION("gatt/notifications"),
        GATT_NOTIFICATION_STOP("gatt/notifications/stop"),
        GATT_CONNECT("gatt/connect"),
        GATT_DISCONNECT("gatt/disconnect"),
        UNKNOWN("unknown");

        private final String command;

        LeCommand(final String command) {
            this.command = command;
        }

        public static LeCommand fromName(final String name) {
            for (LeCommand leCommand : values()) {
                if (leCommand.command.equalsIgnoreCase(name)) {
                    return leCommand;
                }
            }
            return UNKNOWN;
        }

        public String getCommand() {
            return command;
        }

    }

    private CommandResult cmdResSuccess(){
        return new CommandResult(CommandResult.STATUS_COMLETED, new Gson().toJson(new Res(context.getString(R.string.status_ok))));
    }

    private CommandResult cmdResSuccessWithJson(String json) {
        return new CommandResult(CommandResult.STATUS_COMLETED, json);
    }

}