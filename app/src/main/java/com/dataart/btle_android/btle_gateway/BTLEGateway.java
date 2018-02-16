package com.dataart.btle_android.btle_gateway;

import android.content.Context;
import android.os.ParcelUuid;

import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.gateway_helpers.HexHelper;
import com.dataart.btle_android.btle_gateway.gateway_helpers.ValidationHelper;
import com.dataart.btle_android.btle_gateway.model.BTLECharacteristic;
import com.dataart.btle_android.btle_gateway.model.BTLEDevice;
import com.dataart.btle_android.btle_gateway.server.BluetoothServer;
import com.github.devicehive.client.model.Parameter;
import com.github.devicehive.client.service.Device;
import com.github.devicehive.client.service.DeviceCommand;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class BTLEGateway {

    public static final String DEVICE = "device";
    public static final String SERVICE_UUID = "serviceUUID";
    private final BluetoothServer bluetoothServerGateway;
    private Device dhDevice = null;

    public BTLEGateway(BluetoothServer bluetoothServer) {
        this.bluetoothServerGateway = bluetoothServer;
    }

    public void setDhDevice(Device device) {
        this.dhDevice = device;
    }

    private void failWithReason(final Context context, DeviceCommand command, String reason) {
        JsonObject result = new JsonObject();
        result.addProperty(context.getString(R.string.reason), reason);
        command.setResult(result);
        command.setStatus(context.getString(R.string.failed));
        command.updateCommand();
    }

    private void commandStatusResult(final Context context, DeviceCommand command, boolean ok, String description) {
        if (ok) {
            command.setStatus(context.getString(R.string.completed));
        } else {
            command.setStatus(context.getString(R.string.failed));
        }
        if (description != null) {
            if (!description.isEmpty()) {
                JsonObject result = new JsonObject();
                result.addProperty(context.getString(ok ? R.string.result : R.string.reason), description);
                command.setResult(result);
            }
        }
        command.updateCommand();
    }

    private void successWithObject(final Context context, DeviceCommand command, Object object) {
        JsonObject result = new JsonObject();
        result.add(context.getString(R.string.result), new Gson().toJsonTree(object));
        command.setResult(result);
        command.setStatus(context.getString(R.string.completed));
        command.updateCommand();
    }

    public void doCommand(final Context context, DeviceCommand command) {
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

            String validationError;

            Timber.d("switch");
            switch (leCommand) {
                case SCAN_START:
                    bluetoothServerGateway.scanStart();
                    break;

                case SCAN_STOP:
                    bluetoothServerGateway.scanStop();
                    sendStopResult(context, command);
                    break;

                case SCAN:
                    scanAndReturnResults(context, command);
                    return;

                case GATT_CONNECT:
                    validationError = validationHelper.validateAddress(leCommand.getCommand(), address);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    Timber.d("Connecting to %s", address);
                    bluetoothServerGateway.gattConnect(address, () -> {
                        final String data = String.format(context.getString(R.string.is_disconnected), address);
                        sendNotification(context, leCommand, data);
                    }, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
                    return;

                case GATT_DISCONNECT:
                    validationError = validationHelper.validateAddress(leCommand.getCommand(), address);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    Timber.d("Disconnecting from %s", address);
                    bluetoothServerGateway.gattDisconnect(address, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
                    return;

                case GATT_PRIMARY:
                    validationError = validationHelper.validateAddress(leCommand.getCommand(), address);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    gattPrimary(context, address, command, leCommand);
                    return;

                case GATT_CHARACTERISTICS:
                    validationError = validationHelper.validateCharacteristics(leCommand.getCommand(), address, serviceUUID);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    gattCharacteristics(context, address, command, leCommand);
                    return;

                case GATT_READ: {
                    validationError = validationHelper.validateRead(leCommand.getCommand(), address, serviceUUID, characteristicUUID);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    bluetoothServerGateway.gattRead(address, serviceUUID, characteristicUUID, new GattCharacteristicCallBack() {
                        @SuppressWarnings("EmptyMethod")
                        @Override
                        public void onRead(byte[] value) {
//                            no notifications needed
//                            final String sValue = HexHelper.printHexBinary(value);
//                            final String json = new Gson().toJson(sValue);
//                            sendNotification(dh, leCommand, json);
                        }
                    }, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
                    return;
                }

                case GATT_WRITE: {
                    final String sValue = (params != null) ? params.get("value") : null;

                    validationError = validationHelper.validateWrite(leCommand.getCommand(), address, serviceUUID, characteristicUUID, sValue);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    final byte[] value = HexHelper.parseHexBinary(sValue);
                    bluetoothServerGateway.gattWrite(address, serviceUUID, characteristicUUID, value, new GattCharacteristicCallBack() {
                        @Override
                        public void onWrite(int state) {
//                            no notifications needed
//                            final String json = new Gson().toJson(state);
//                            sendNotification(dh, leCommand, json);
                        }
                    }, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
                    return;
                }

                case GATT_NOTIFICATION:
                    validationError = validationHelper.validateNotifications(leCommand.getCommand(), address, serviceUUID);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    bluetoothServerGateway.gattNotifications(context, address, serviceUUID, characteristicUUID, true, new GattCharacteristicCallBack() {
                        @Override
                        public void onRead(byte[] value) {
                            final String sValue = HexHelper.printHexBinary(value);
                            sendNotification(context, leCommand, sValue);
                        }
                    }, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
                    return;

                case GATT_NOTIFICATION_STOP:
                    validationError = validationHelper.validateNotifications(leCommand.getCommand(), address, serviceUUID);
                    if (validationError != null) {
                        failWithReason(context, command, validationError);
                        return;
                    }

                    bluetoothServerGateway.gattNotifications(context, address, serviceUUID, characteristicUUID, false, new GattCharacteristicCallBack() {

                        @Override
                        public void onRead(byte[] value) {
                            final String sValue = HexHelper.printHexBinary(value);
                            sendNotification(context, leCommand, sValue);
                        }
                    }, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
                    return;

                case UNKNOWN:
                default:
                    failWithReason(context, command, context.getString(R.string.unknown_command));
                    return;
            }
        } catch (Exception e) {
            Timber.e("error: %s", e.toString());
            failWithReason(context, command, e.toString());
            return;
        }

        Timber.d("default status ok");
        command.setStatus(context.getString(R.string.completed));
        command.updateCommand();
    }

    private void scanAndReturnResults(Context context, DeviceCommand command) {
        bluetoothServerGateway.scanStart();
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(BluetoothServer.COMMAND_SCAN_DELAY);
                    sendStopResult(context, command);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void sendNotification(Context context, final LeCommand leCommand, final String data) {
        Timber.d("Notification: %s", data);
        if (dhDevice != null) {
            ArrayList<Parameter> parameters = new ArrayList<>();
            parameters.add(new Parameter(context.getString(R.string.data), data));
            dhDevice.sendNotification(leCommand.getCommand(), parameters);
        }
    }

    private void sendStopResult(Context context, DeviceCommand command) {
        final ArrayList<BTLEDevice> devices = bluetoothServerGateway.getDiscoveredDevices();
        successWithObject(context, command, devices);
    }

    private void gattPrimary(Context context, String address, DeviceCommand command, @SuppressWarnings("UnusedParameters") final LeCommand leCommand) {
        bluetoothServerGateway.gattPrimary(address, new GattCharacteristicCallBack() {
            @Override
            public void onServices(List<ParcelUuid> uuidList) {
                successWithObject(context, command, uuidList);
            }
        }, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
    }

    private void gattCharacteristics(Context context, String address, DeviceCommand command, @SuppressWarnings("UnusedParameters") final LeCommand leCommand) {
        bluetoothServerGateway.gattCharacteristics(address, new GattCharacteristicCallBack() {
            @Override
            public void onCharacteristics(ArrayList<BTLECharacteristic> characteristics) {
                successWithObject(context, command, characteristics);
            }
        }, (boolean ok, String reason) -> commandStatusResult(context, command, ok, reason));
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
