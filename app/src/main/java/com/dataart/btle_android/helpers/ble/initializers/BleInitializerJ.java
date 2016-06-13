package com.dataart.btle_android.helpers.ble.initializers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import com.dataart.btle_android.helpers.ble.base.BleInitializer;

import lombok.Data;

/**
 * Created by Constantine Mars on 12/13/15.
 * <p>
 * Jelly Bean MR2 - specific BLE helper
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@Data
public class BleInitializerJ extends BleInitializer {

    public BleInitializerJ(Activity activity, InitCompletionCallback initCompletionCallback) {
        super(activity, initCompletionCallback);

        init();
    }

    @Override
    public void start() {
        enableBluetooth();
    }
}
