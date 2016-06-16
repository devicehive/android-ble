package com.dataart.btle_android.helpers.ble.initializers;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;

import com.dataart.btle_android.R;
import com.dataart.btle_android.helpers.LocationHelper;
import com.dataart.btle_android.helpers.PermissionsHelper;
import com.dataart.btle_android.helpers.ble.base.BleInitializer;

import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by Constantine Mars on 12/13/15.
 * <p>
 * Marshmallow-specific BLE helper
 */
public class BleInitializerM extends BleInitializerL {
    private static String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private Action1<String[]> onError;
    private Action1<String> onSuccess;
    private LocationHelper locationHelper;

    public BleInitializerM(Activity activity, BleInitializer.InitCompletionCallback initCompletionCallback) {
        super(activity, initCompletionCallback);
        locationHelper = new LocationHelper(this::callSuperStart, activity);

        onError = permissions -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.no_permissions_title);
            builder.setMessage(R.string.no_permissions_message);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(null);
            builder.show();
        };
        onSuccess = s -> {
            enableLocation();
            Timber.i(s);
        };
    }

    /**
     * 1. Request permissions at runtime (Android M specific)
     */
    @Override
    public void start() {
        requestPermissions();
    }

    /**
     * 2. Enable location (switch location on if it's still not on)
     */
    private void enableLocation() {
        locationHelper.checkLocationEnabled();
    }

    /**
     * 3. Call super.start() to complete sequence (enable bluetooth and call initCompletionCallback.onComplete())
     */
    private void callSuperStart() {
        super.start();
    }

    public void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionsHelper.checkPermissions(
                    activity,
                    permissions,
                    activity.getString(R.string.permissions_explanation))) {
                enableLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        PermissionsHelper.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults,
                onSuccess,
                onError);
    }

    @Override
    public void onStart() {
        super.onStart();
        locationHelper.onStart();
    }

    @Override
    public void onStop() {
        locationHelper.onStop();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        locationHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        locationHelper.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
