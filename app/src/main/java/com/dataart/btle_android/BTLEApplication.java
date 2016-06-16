package com.dataart.btle_android;

import android.support.multidex.MultiDexApplication;

import com.dataart.btle_android.devicehive.BTLEDeviceHive;

/**
 * Created by alrybakov
 */

public class BTLEApplication extends MultiDexApplication {

    private static BTLEApplication application;

    private BTLEDeviceHive device;

    public static BTLEApplication getApplication() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        application = this;
        device = BTLEDeviceHive.newInstance(getApplicationContext());
    }

    public BTLEDeviceHive getDevice() {
        return device;
    }

}
