package com.dataart.btle_android.btle_helper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

/**
 * Created by Constantine Mars on 12/15/15.
 */
@TargetApi(Build.VERSION_CODES.M)
public class PermissionsHelper {
    public static final int PERMISSION_REQUEST = 2001;

    public static void checkPermissions(Activity activity, String[] permissions, String message) {
        List<String> permissionsToCheck = new ArrayList<>();
        rx.Observable.from(permissions)
                .filter(permission -> activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                .forEach(permissionsToCheck::add);

        if (permissionsToCheck.size() > 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("This app needs some permissions");
            builder.setMessage(message);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> activity.requestPermissions(permissionsToCheck.toArray(new String[permissionsToCheck.size()]), PERMISSION_REQUEST));
            builder.show();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults,
                                                  Action1<String> onSuccess, Action1<String[]> onError) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                List<String> permissionsNotGranted = new ArrayList<>();

                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        permissionsNotGranted.add(permissions[i]);
                    }
                }

                if (permissionsNotGranted.size() > 0) {
                    onError.call(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]));
                } else {
                    onSuccess.call("All needed permissions granted");
                }
            }
        }
    }
}
