package com.dataart.btle_android;

import android.app.Application;

import com.dataart.btle_android.devicehive.BTLEDeviceHive;

/**
 * Created by alrybakov
 */
public class BTLEApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        device = BTLEDeviceHive.newInstance(getApplicationContext());
    }

    BTLEDeviceHive device;

    public BTLEDeviceHive getDevice() {
        return device;
    }
}
