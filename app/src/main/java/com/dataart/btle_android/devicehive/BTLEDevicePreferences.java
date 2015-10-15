package com.dataart.btle_android.devicehive;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.dataart.btle_android.BTLEApplication;

public class BTLEDevicePreferences {

    private final static String NAMESPACE = "devicehive.";

    private final static String KEY_SERVER_URL = NAMESPACE
            .concat(".KEY_SERVER_URL");

    private final static String KEY_GATEWAY_ID = NAMESPACE
            .concat(".KEY_GATEWAY_ID");

    private final static String KEY_ACCESSKEY= NAMESPACE
            .concat(".KEY_ACCESSKEY");

    private final Context context;

    private final SharedPreferences preferences;

    public BTLEDevicePreferences() {

        this.context = BTLEApplication.getApplication();
        this.preferences = context.getSharedPreferences(
                context.getPackageName() + "_devicehiveprefs",
                Context.MODE_PRIVATE);
    }

    public String getServerUrl() {
        return preferences.getString(KEY_SERVER_URL, null);
    }

    public String getGatewayId() {
        return preferences.getString(KEY_GATEWAY_ID, null);
    }

    public void setServerUrlSync(String serverUrl) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SERVER_URL, serverUrl);
        editor.commit();
    }

    public void setGatewayIdSync(String gatewayId) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_GATEWAY_ID, gatewayId);
        editor.commit();
    }

    public String getAccessKey() {
        return preferences.getString(KEY_ACCESSKEY, null);
    }

    public void setAccessKeySync(String accessKey) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_ACCESSKEY, accessKey);
        editor.commit();
    }

    public void setCredentialsAsync(final String accessKey) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                setAccessKeySync(accessKey);
                return null;
            }

        }.execute();
    }
}
