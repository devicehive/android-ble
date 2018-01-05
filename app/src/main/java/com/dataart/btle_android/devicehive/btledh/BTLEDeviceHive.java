package com.dataart.btle_android.devicehive.btledh;

import android.os.Build;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;
import com.dataart.btle_android.devicehive.DeviceHiveConfig;
import com.github.devicehive.client.model.DeviceNotification;
import com.github.devicehive.client.service.DeviceCommand;
import com.github.devicehive.client.service.DeviceHive;

import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

public class BTLEDeviceHive {

    private DeviceHive deviceHive;
    private BTLEDevicePreferences prefs;

    private List<RegistrationListener> registrationListeners = new LinkedList<>();
    private CommandListener commandListener;
    private List<NotificationListener> notificationListeners = new LinkedList<>();

    public BTLEDeviceHive() {
        prefs = BTLEDevicePreferences.getInstance();
        deviceHive = DeviceHive.getInstance().init(prefs.getServerUrl(), prefs.getRefreshToken());
    }

    public DeviceHive getDeviceHive() {
        if (deviceHive == null) {
            deviceHive.init(prefs.getServerUrl(), prefs.getRefreshToken());
        }
        return this.deviceHive;
    }

    public static String getDeviceName() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        return model.startsWith(manufacturer) ? model : manufacturer + " " + model;
    }

    public void onBeforeRunCommand(DeviceCommand command) {
        Timber.d("onBeforeRunCommand: " + command.getCommandName());
    }

    public SimpleCallableFuture<CommandResult> runCommand(DeviceCommand command) {
        Timber.d("Executing command on test device: " + command.getCommandName());
        return notifyListenersCommandReceived(command);
    }

    public boolean shouldRunCommandAsynchronously(DeviceCommand command) {
        return true;
    }

    public void addDeviceListener(RegistrationListener listener) {
        registrationListeners.add(listener);
    }

    public void removeDeviceListener(RegistrationListener listener) {
        registrationListeners.remove(listener);
    }

    public void setCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

    public void removeCommandListener() {
        commandListener = null;
    }

    protected void onStartRegistration() {
        Timber.d("onStartRegistration");
    }

    protected void onFinishRegistration() {
        Timber.d("onFinishRegistration");
//        isRegistered = true;
        notifyListenersDeviceRegistered();
    }

    protected void onFailRegistration() {
        Timber.d("onFailRegistration");
        notifyListenersDeviceFailedToRegister();
    }

    protected void onStartProcessingCommands() {
        Timber.d("onStartProcessingCommands");
    }

    protected void onStopProcessingCommands() {
        Timber.d("onStopProcessingCommands");
    }

    protected void onStartSendingNotification(DeviceNotification notification) {
        Timber.d("onStartSendingNotification : " + notification.getNotification());
    }

    protected void onFinishSendingNotification(DeviceNotification notification) {
        Timber.d("onFinishSendingNotification : " + notification.getNotification());
        notifyListenersDeviceSentNotification(notification);
    }

    protected void onFailSendingNotification(DeviceNotification notification) {
        Timber.d("onFailSendingNotification : " + notification.getNotification());
        notifyListenersDeviceFailedToSendNotification(notification);
    }

    private SimpleCallableFuture<CommandResult> notifyListenersCommandReceived(DeviceCommand command) {
        return commandListener.onDeviceReceivedCommand(command);
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

    private void notifyListenersDeviceSentNotification(DeviceNotification notification) {
        for (NotificationListener listener : notificationListeners) {
            listener.onDeviceSentNotification(notification);
        }
    }

    private void notifyListenersDeviceFailedToSendNotification(
            DeviceNotification notification) {
        for (NotificationListener listener : notificationListeners) {
            listener.onDeviceFailedToSendNotification(notification);
        }
    }


    public static BTLEDeviceHive newInstance() {
        BTLEDevicePreferences prefs = BTLEDevicePreferences.getInstance();

        BTLEDeviceHive device = new BTLEDeviceHive();

        device.getDeviceHive().enableDebug(true);

        String serverUrl = prefs.getServerUrl();

        if (serverUrl == null) {
            serverUrl = DeviceHiveConfig.API_ENDPOINT;
            prefs.setServerUrlSync(serverUrl);
        }
        return device;
    }

}
