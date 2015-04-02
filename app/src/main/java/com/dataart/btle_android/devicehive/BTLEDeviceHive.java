package com.dataart.btle_android.devicehive;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.DeviceClass;
import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Network;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.Device;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.future.SimpleCallableFuture;

import java.util.LinkedList;
import java.util.List;

public class BTLEDeviceHive extends Device {

    private static final String TAG = "AndroidBTLE";

    private List<RegistrationListener> registrationListeners = new LinkedList<RegistrationListener>();
//    private List<CommandListener> commandListeners = new LinkedList<CommandListener>();
    private CommandListener commandListener;
    private List<NotificationListener> notificationListeners = new LinkedList<NotificationListener>();

    public interface RegistrationListener {
        void onDeviceRegistered();

        void onDeviceFailedToRegister();
    }

    public interface CommandListener {
        SimpleCallableFuture<CommandResult> onDeviceReceivedCommand(Command command);
    }

    public interface NotificationListener {
        void onDeviceSentNotification(Notification notification);

        void onDeviceFailedToSendNotification(Notification notification);
    }

    public BTLEDeviceHive(Context context) {
        super(context, getTestDeviceData());
        attachEquipment(new BTLEEquipment());

    }

    private static DeviceData getTestDeviceData() {
        final Network network = new Network("AndroidBTLE", "");
        final DeviceClass deviceClass = new DeviceClass("c.mars_device.class", "1.0");//AndroidBTLE Device", "1.0");

        return new DeviceData(
                new BTLEDevicePreferences().getGatewayId(),
                "582c2008-cbb6-4b1a-8cf1-7cec1388db9f",
                getDeviceName(),
                DeviceData.DEVICE_STATUS_ONLINE, network, deviceClass);
    }

    public static String getDeviceName() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        return model.startsWith(manufacturer) ? model : manufacturer + " " + model;
    }

    @Override
    public void onBeforeRunCommand(Command command) {
        Log.d(TAG, "onBeforeRunCommand: " + command.getCommand());
//        notifyListenersCommandReceived wasn't returning command result before
//        notifyListenersCommandReceived(command);
    }

    @Override
    public SimpleCallableFuture<CommandResult> runCommand(final Command command) {
        Log.d(TAG, "Executing command on test device: " + command.getCommand());
        return notifyListenersCommandReceived(command);
    }

    @Override
    public boolean shouldRunCommandAsynchronously(final Command command) {
        return true;
    }

    public void addDeviceListener(RegistrationListener listener) {
        registrationListeners.add(listener);
    }

    public void removeDeviceListener(RegistrationListener listener) {
        registrationListeners.remove(listener);
    }

//    FIXME: should it be removed?
//    public void addCommandListener(CommandListener listener) {
//        commandListeners.add(listener);
//    }

    public void setCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

//    FIXME: should be removed?
//    public void removeCommandListener(CommandListener listener) {
//        commandListeners.remove(listener);
//    }

    public void removeCommandListener() {
        commandListener = null;
    }

//    FIXME: unused
//    public void addNotificationListener(NotificationListener listener) {
//        notificationListeners.add(listener);
//    }
//
//    public void removeNotificationListener(NotificationListener listener) {
//        notificationListeners.remove(listener);
//    }
//
//    public void removeListener(Object listener) {
//        registrationListeners.remove(listener);
//        commandListeners.remove(listener);
//        notificationListeners.remove(listener);
//    }

    @Override
    protected void onStartRegistration() {
        Log.d(TAG, "onStartRegistration");
    }

    @Override
    protected void onFinishRegistration() {
        Log.d(TAG, "onFinishRegistration");
        isRegistered = true;
        notifyListenersDeviceRegistered();
    }

    @Override
    protected void onFailRegistration() {
        Log.d(TAG, "onFailRegistration");
        notifyListenersDeviceFailedToRegister();
    }

    @Override
    protected void onStartProcessingCommands() {
        Log.d(TAG, "onStartProcessingCommands");
    }

    @Override
    protected void onStopProcessingCommands() {
        Log.d(TAG, "onStopProcessingCommands");
    }

    @Override
    protected void onStartSendingNotification(Notification notification) {
        Log.d(TAG, "onStartSendingNotification : " + notification.getName());
    }

    @Override
    protected void onFinishSendingNotification(Notification notification) {
        Log.d(TAG, "onFinishSendingNotification : " + notification.getName());
        notifyListenersDeviceSentNotification(notification);
    }

    @Override
    protected void onFailSendingNotification(Notification notification) {
        Log.d(TAG, "onFailSendingNotification : " + notification.getName());
        notifyListenersDeviceFailedToSendNotification(notification);
    }

    private SimpleCallableFuture<CommandResult> notifyListenersCommandReceived(Command command) {
//        for (CommandListener listener : commandListeners) {
            return commandListener.onDeviceReceivedCommand(command);
//            Should return immediately if error happened
//            if (!result.getStatus().equals(CommandResult.STATUS_COMLETED)) {
//                return result;
//            }
//        }
//        return new CommandResult(CommandResult.STATUS_COMLETED, getContext().getString(R.string.status_ok));
    }

    private void notifyListenersDeviceRegistered() {
        for (RegistrationListener listener : registrationListeners) {
            listener.onDeviceRegistered();
        }
    }

    private void notifyListenersDeviceFailedToRegister() {
        for (RegistrationListener listener : registrationListeners) {
            listener.onDeviceFailedToRegister();
        }
    }

    private void notifyListenersDeviceSentNotification(Notification notification) {
        for (NotificationListener listener : notificationListeners) {
            listener.onDeviceSentNotification(notification);
        }
    }

    private void notifyListenersDeviceFailedToSendNotification(
            Notification notification) {
        for (NotificationListener listener : notificationListeners) {
            listener.onDeviceFailedToSendNotification(notification);
        }
    }


    public static BTLEDeviceHive newInstance(Context context) {

        final BTLEDeviceHive device = new BTLEDeviceHive(context);
        device.setDebugLoggingEnabled(true);

        final BTLEDevicePreferences prefs = new BTLEDevicePreferences();
        String serverUrl = prefs.getServerUrl();

        if (serverUrl == null) {
            serverUrl = DeviceHiveConfig.API_ENDPOINT;
            prefs.setServerUrlSync(serverUrl);
        }
        device.setApiEnpointUrl(serverUrl);
        return device;
    }

}
