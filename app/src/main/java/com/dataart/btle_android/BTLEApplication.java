package com.dataart.btle_android;

import android.app.Application;

import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

/**
 * Created by alrybakov
 */

public class BTLEApplication extends Application {

    private static BTLEApplication application;


    public static BTLEApplication getApplication() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        BTLEDevicePreferences.getInstance().init(this);
    }

}
