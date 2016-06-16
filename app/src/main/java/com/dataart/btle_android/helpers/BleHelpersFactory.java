package com.dataart.btle_android.helpers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;

import com.dataart.btle_android.helpers.ble.base.BleInitializer;
import com.dataart.btle_android.helpers.ble.base.BleScanner;
import com.dataart.btle_android.helpers.ble.initializers.BleInitializerJ;
import com.dataart.btle_android.helpers.ble.initializers.BleInitializerL;
import com.dataart.btle_android.helpers.ble.initializers.BleInitializerM;
import com.dataart.btle_android.helpers.ble.scanners.BleScannerJ;
import com.dataart.btle_android.helpers.ble.scanners.BleScannerL;

/**
 * Created by Constantine Mars on 12/13/15.
 * Factory for creating BLE init helper according to OS version
 */
public class BleHelpersFactory {

    public static BleInitializer getInitializer(Activity activity, BleInitializer.InitCompletionCallback initCompletionCallback) {
        final int osVersion = Build.VERSION.SDK_INT;

        if (osVersion >= Build.VERSION_CODES.M) {
            return new BleInitializerM(activity, initCompletionCallback);
        } else if (osVersion >= Build.VERSION_CODES.LOLLIPOP) {
            return new BleInitializerL(activity, initCompletionCallback);
        } else if (osVersion >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new BleInitializerJ(activity, initCompletionCallback);
        }

//        There is no BLE support in Android versions below Jelly Bean
        return null;
    }

    public static BleScanner getScanner(BleScanner.ScanCallback scanCallback, BluetoothAdapter bluetoothAdapter) {
        final int osVersion = Build.VERSION.SDK_INT;

        if (osVersion >= Build.VERSION_CODES.LOLLIPOP) {
            return new BleScannerL(scanCallback, bluetoothAdapter);
        } else if (osVersion >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new BleScannerJ(scanCallback, bluetoothAdapter);
        }

//        There is no BLE support in Android versions below Jelly Bean
        return null;
    }
}
