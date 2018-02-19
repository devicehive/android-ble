package com.dataart.btle_android.devicehive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class BTLEDevicePreferences {

    private final static String NAMESPACE = "devicehive.";

    private final static String KEY_SERVER_URL = NAMESPACE
            .concat(".KEY_SERVER_URL");

    private final static String KEY_GATEWAY_ID = NAMESPACE
            .concat(".KEY_GATEWAY_ID");

    private final static String KEY_REFRESH_TOKEN = NAMESPACE
            .concat(".KEY_REFRESH_TOKEN");


    private SharedPreferences preferences;

    private BTLEDevicePreferences() {
    }
    public void init(Context context) {
        this.preferences = context.getSharedPreferences(
                context.getPackageName() + "_devicehiveprefs",
                Context.MODE_PRIVATE);
    }
    public static BTLEDevicePreferences getInstance() {
        return BTLEDevicePreferences.InstanceHolder.INSTANCE;
    }

    private static class InstanceHolder {
        static final BTLEDevicePreferences INSTANCE = new BTLEDevicePreferences();
    }

    public void clearPreferences() {
        preferences.edit().clear().apply();
    }

    public String getServerUrl() {
        return preferences.getString(KEY_SERVER_URL, null);
    }

    public String getGatewayId() {
        return preferences.getString(KEY_GATEWAY_ID, null);
    }

    @SuppressLint("ApplySharedPref")
    public void setServerUrlSync(String serverUrl) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SERVER_URL, serverUrl);
        editor.commit();
    }
    @SuppressLint("ApplySharedPref")
    public void setGatewayIdSync(String gatewayId) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_GATEWAY_ID, gatewayId);
        editor.commit();
    }

    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, null);
    }

    @SuppressLint("ApplySharedPref")
    public void setRefreshTokenSync(String refreshToken) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.commit();
    }
}
