package com.dataart.btle_android.btle_gateway;

import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;

import com.dataart.btle_android.devicehive.btledh.CommandResult;
import com.dataart.btle_android.devicehive.btledh.CmdResFuture;
import com.dataart.btle_android.devicehive.btledh.SimpleCallableFuture;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.gateway_helpers.HexHelper;
import com.dataart.btle_android.btle_gateway.gateway_helpers.ValidationHelper;
import com.dataart.btle_android.btle_gateway.gatt_callbacks.CmdResult;
import com.dataart.btle_android.btle_gateway.model.BTLECharacteristic;
import com.dataart.btle_android.btle_gateway.model.BTLEDevice;
import com.dataart.btle_android.btle_gateway.server.BluetoothServer;
import com.dataart.btle_android.devicehive.btledh.BTLEDeviceHive;
import com.github.devicehive.client.service.DeviceCommand;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class BTLEGateway {

    public static final String DEVICE = "device";
    public static final String SERVICE_UUID = "serviceUUID";
    private BluetoothServer bluetoothServerGateway;

    public BTLEGateway(BluetoothServer bluetoothServer) {
        this.bluetoothServerGateway = bluetoothServer;
    }

    public SimpleCallableFuture<CommandResult> doCommand(final Context context, final BTLEDeviceHive dh, final DeviceCommand command) {
        ValidationHelper validationHelper = new ValidationHelper(context);

        try {
            Timber.d("doCommand");
            final String name = command.getCommandName();
            final LeCommand leCommand = LeCommand.fromName(name);

            Type type = new TypeToken<HashMap<String, String>>() {
            }.getType();
            HashMap<String, String> params = new Gson().fromJson(command.getParameters().toString(), type);

            final String address = (params != null) ? params.get(DEVICE) : null;
            final String serviceUUID = (params != null) ? params.get(SERVICE_UUID) : null;
            final String characteristicUUID = (params != null) ? params.get("characteristicUUID") : null;

            Optional<CmdResFuture> validationError;

            Timber.d("switch");
            switch (leCommand) {
                case SCAN_START:
                    bluetoothServerGateway.scanStart();
                    break;

                case SCAN_STOP:
                    bluetoothServerGateway.scanStop();
                    sendStopResult(dh);
                    break;

                case SCAN:
                    return scanAndReturnResults(dh);

                case GATT_CONNECT:
                    validationError = validationHelper.validateAddress(leCommand.getCommand(), address);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    Timber.d("Connecting to " + address);
                    return bluetoothServerGateway.gattConnect(address, () -> {
                        final String json = new Gson().toJson(String.format(context.getString(R.string.is_disconnected), address));
                        sendNotification(dh, leCommand, json);
                    });

                case GATT_DISCONNECT:
                    validationError = validationHelper.validateAddress(leCommand.getCommand(), address);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    Timber.d("Disconnecting from" + address);
                    return bluetoothServerGateway.gattDisconnect(address);

                case GATT_PRIMARY:
                    validationError = validationHelper.validateAddress(leCommand.getCommand(), address);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    return gattPrimary(address, dh, leCommand);

                case GATT_CHARACTERISTICS:
                    validationError = validationHelper.validateCharacteristics(leCommand.getCommand(), address, serviceUUID);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    return gattCharacteristics(address, dh, leCommand);

                case GATT_READ: {
                    validationError = validationHelper.validateRead(leCommand.getCommand(), address, serviceUUID, characteristicUUID);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    return bluetoothServerGateway.gattRead(address, serviceUUID, characteristicUUID, new GattCharacteristicCallBack() {
                        @Override
                        public void onRead(byte[] value) {
//                            no notifications needed
//                            final String sValue = HexHelper.printHexBinary(value);
//                            final String json = new Gson().toJson(sValue);
//                            sendNotification(dh, leCommand, json);
                        }
                    });
                }

                case GATT_WRITE: {
                    final String sValue = (String) (params != null ? params.get("value") : null);

                    validationError = validationHelper.validateWrite(leCommand.getCommand(), address, serviceUUID, characteristicUUID, sValue);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    final byte[] value = HexHelper.parseHexBinary(sValue);
                    return bluetoothServerGateway.gattWrite(address, serviceUUID, characteristicUUID, value, new GattCharacteristicCallBack() {
                        @Override
                        public void onWrite(int state) {
//                            no notifications needed
//                            final String json = new Gson().toJson(state);
//                            sendNotification(dh, leCommand, json);
                        }
                    });
                }

                case GATT_NOTIFICATION:
                    validationError = validationHelper.validateNotifications(leCommand.getCommand(), address, serviceUUID);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    return bluetoothServerGateway.gattNotifications(context, address, serviceUUID, characteristicUUID, true, new GattCharacteristicCallBack() {
                        @Override
                        public void onRead(byte[] value) {
                            final String sValue = HexHelper.printHexBinary(value);
                            final String json = new Gson().toJson(sValue);
                            sendNotification(dh, leCommand, json);
                        }
                    });

                case GATT_NOTIFICATION_STOP:
                    validationError = validationHelper.validateNotifications(leCommand.getCommand(), address, serviceUUID);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    return bluetoothServerGateway.gattNotifications(context, address, serviceUUID, characteristicUUID, false, new GattCharacteristicCallBack() {

                        @Override
                        public void onRead(byte[] value) {
                            final String sValue = HexHelper.printHexBinary(value);
                            final String json = new Gson().toJson(sValue);
                            sendNotification(dh, leCommand, json);
                        }
                    });

                case UNKNOWN:
                default:
                    return new CmdResFuture(CmdResult.failWithStatus(R.string.unknown_command));
            }
        } catch (Exception e) {
            Timber.e("error:" + e.toString());

//TODO To what device should we send notification
//            final Notification notification = new Notification("Error", e.toString());
//            dh.sendNotification(notification);

            return new SimpleCallableFuture<>(CmdResult.failWithStatus("Error: \"" + e.toString() + "\""));
        }

        Timber.d("default status ok");
        return new SimpleCallableFuture<>(CmdResult.success());
    }

    private SimpleCallableFuture<CommandResult> scanAndReturnResults(final BTLEDeviceHive dh) {
        final SimpleCallableFuture<CommandResult> future = new SimpleCallableFuture<>();

        bluetoothServerGateway.scanStart();
        Handler handler = new Handler();
        handler.postDelayed(() -> sendStopResult(dh, future), BluetoothServer.COMMAND_SCAN_DELAY);

        return future;
    }

    private void sendNotification(final BTLEDeviceHive dh, final LeCommand leCommand, final String json) {
//        final Notification notification = new Notification(leCommand.getCommand(), json);
//        dh.sendNotification(notification);
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
        if (future != null) {
            future.call(new CommandResult(CommandResult.STATUS_COMPLETED, json));
        }

//        final Notification notification = new Notification("discoveredDevices", result);


//        dh.sendNotification(notification);
    }

    private SimpleCallableFuture<CommandResult> gattPrimary(String address, @SuppressWarnings("UnusedParameters") final BTLEDeviceHive dh, @SuppressWarnings("UnusedParameters") final LeCommand leCommand) {
        final CmdResFuture future = new CmdResFuture();
        bluetoothServerGateway.gattPrimary(address, new GattCharacteristicCallBack() {
            @Override
            public void onServices(List<ParcelUuid> uuidList) {
                future.call(CmdResult.successWithObject(uuidList));
            }
        }, future);
        return future;
    }

    private SimpleCallableFuture<CommandResult> gattCharacteristics(String address, @SuppressWarnings("UnusedParameters") final BTLEDeviceHive dh, @SuppressWarnings("UnusedParameters") final LeCommand leCommand) {
        final CmdResFuture future = new CmdResFuture();
        bluetoothServerGateway.gattCharacteristics(address, new GattCharacteristicCallBack() {
            @Override
            public void onCharacteristics(ArrayList<BTLECharacteristic> characteristics) {
                future.call(CmdResult.successWithObject(characteristics));
            }
        }, future);
        return future;
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

}