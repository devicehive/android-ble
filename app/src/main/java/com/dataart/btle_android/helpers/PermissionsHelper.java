package com.dataart.btle_android.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;

import com.dataart.btle_android.R;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;


/**
 * Created by Constantine Mars on 12/15/15.
 */
@TargetApi(Build.VERSION_CODES.M)
public class PermissionsHelper {
    private static final int PERMISSION_REQUEST = 2001;

    public static boolean checkPermissions(Activity activity, String[] permissions, String message) {
        List<String> permissionsToCheck = new ArrayList<>();

        Observable.fromArray(permissions)
                .filter(permission -> activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                .forEach(permissionsToCheck::add);

        if (permissionsToCheck.isEmpty()) {
            return true;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.needs_permissions);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(dialog -> activity.requestPermissions(
                permissionsToCheck.toArray(
                        new String[permissionsToCheck.size()]
                ), PERMISSION_REQUEST));
        builder.show();

        return false;
    }

    public static void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults,
                                                  Consumer<String> onSuccess, Consumer<String[]> onError) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                List<String> permissionsNotGranted = new ArrayList<>();

                for (String permission : permissions) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        permissionsNotGranted.add(permission);
                    }
                }
                try {
                    if (permissionsNotGranted.size() > 0) {

                        onError.accept(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]));

                    } else {
                        onSuccess.accept("All required permissions granted");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
