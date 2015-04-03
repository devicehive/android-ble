package com.dataart.btle_android;

import android.app.Application;

import com.dataart.btle_android.devicehive.BTLEDeviceHive;

/**
 * Created by alrybakov
 */

public class BTLEApplication extends Application {

    private static BTLEApplication application;

    private BTLEDeviceHive device;

    @Override
    public void onCreate() {
        super.onCreate();

        application = this;
        device = BTLEDeviceHive.newInstance(getApplicationContext());
    }

    public static BTLEApplication getApplication() {
        return application;
    }

    public BTLEDeviceHive getDevice() {
        return device;
    }

}
