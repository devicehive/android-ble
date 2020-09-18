package com.dataart.btle_android;

import android.app.Application;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;
import timber.log.Timber;

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
        if (BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree());
        }
    }

}
