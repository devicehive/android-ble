package com.dataart.btle_android.helpers.ble.initializers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import lombok.Data;

/**
 * Created by Constantine Mars on 12/6/15.
 * <p>
 * Lollipop-specific BLE Helper
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@Data
public class BleInitializerL extends BleInitializerJ {

    public BleInitializerL(Activity activity, InitCompletionCallback initCompletionCallback) {
        super(activity, initCompletionCallback);
    }

}
