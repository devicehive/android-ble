package com.dataart.btle_android.devicehive;

import android.os.Build;
import android.util.Log;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.github.devicehive.client.model.DeviceNotification;
import com.github.devicehive.client.service.DeviceCommand;
import com.github.devicehive.client.service.DeviceHive;

import java.util.LinkedList;
import java.util.List;

public class BTLEDeviceHive {

    private static final String TAG = "AndroidBTLE";
    private DeviceHive deviceHive;
    private BTLEDevicePreferences prefs;

    private List<RegistrationListener> registrationListeners = new LinkedList<>();
    private CommandListener commandListener;
    private List<NotificationListener> notificationListeners = new LinkedList<>();

    public interface RegistrationListener {
        void onDeviceRegistered();

        void onDeviceFailedToRegister();
    }

    public interface CommandListener {
        SimpleCallableFuture<CommandResult> onDeviceReceivedCommand(DeviceCommand command);
    }

    public interface NotificationListener {
        void onDeviceSentNotification(DeviceNotification notification);

        void onDeviceFailedToSendNotification(DeviceNotification notification);
    }

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
        Log.d(TAG, "onBeforeRunCommand: " + command.getCommandName());
    }

    public SimpleCallableFuture<CommandResult> runCommand(DeviceCommand command) {
        Log.d(TAG, "Executing command on test device: " + command.getCommandName());
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
        Log.d(TAG, "onStartRegistration");
    }

    protected void onFinishRegistration() {
        Log.d(TAG, "onFinishRegistration");
//        isRegistered = true;
        notifyListenersDeviceRegistered();
    }

    protected void onFailRegistration() {
        Log.d(TAG, "onFailRegistration");
        notifyListenersDeviceFailedToRegister();
    }

    protected void onStartProcessingCommands() {
        Log.d(TAG, "onStartProcessingCommands");
    }

    protected void onStopProcessingCommands() {
        Log.d(TAG, "onStopProcessingCommands");
    }

    protected void onStartSendingNotification(DeviceNotification notification) {
        Log.d(TAG, "onStartSendingNotification : " + notification.getNotification());
    }

    protected void onFinishSendingNotification(DeviceNotification notification) {
        Log.d(TAG, "onFinishSendingNotification : " + notification.getNotification());
        notifyListenersDeviceSentNotification(notification);
    }

    protected void onFailSendingNotification(DeviceNotification notification) {
        Log.d(TAG, "onFailSendingNotification : " + notification.getNotification());
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
