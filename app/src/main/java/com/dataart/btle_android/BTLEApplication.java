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

        application = this;
        device = BTLEDeviceHive.newInstance(getApplicationContext());

    }

    static BTLEApplication application;
    public static BTLEApplication getApplication() {
        return application;
    }

    BTLEDeviceHive device;

    public BTLEDeviceHive getDevice() {
        return device;
    }


}
