package com.dataart.btle_android.blehelper.helpers;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;

import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by Constantine Mars on 12/13/15.
 */
public class BleHelperMarshmallow extends BleHelperLollipop {
    private static String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    Action1<String[]> onError;
    Action1<String> onSuccess;

    public BleHelperMarshmallow(Activity activity, ScanListener listener) {
        super(activity, listener);

        onError = permissions -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Functionality limited");
            builder.setMessage("Since location access has not been granted, this app will not be able to discover BLE devices when in the background.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> {
            });
            builder.show();
        };
        onSuccess = s -> Timber.i(s);
    }

    @Override
    public void scan(boolean enable) {
        this.onSuccess = s -> super.scan(enable);
        checkAndRequestPermissions();
    }

    public void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionsHelper.checkPermissions(activity,
                    permissions,
                    "Application needs Bluetooth Admin and Location permissions to scan for BLE devices");
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        PermissionsHelper.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults,
                onSuccess,
                onError);
    }
}
