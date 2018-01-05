package com.dataart.btle_android.devicehive.btledh;

import com.github.devicehive.client.model.DeviceNotification;

public interface NotificationListener {
    void onDeviceSentNotification(DeviceNotification notification);

    void onDeviceFailedToSendNotification(DeviceNotification notification);
}
