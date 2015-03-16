package com.dataart.btle_android.devicehive;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.dataart.btle_android.BTLEApplication;

public class BTLEDevicePreferences {

    private final static String NAMESPACE = "devicehive.";

    private final static String KEY_SERVER_URL = NAMESPACE
            .concat(".KEY_SERVER_URL");

    private final static String KEY_USERNAME = NAMESPACE
            .concat(".KEY_USERNAME");

    private final static String KEY_PASSWORD = NAMESPACE
            .concat(".KEY_PASSWORD");

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

    public void setServerUrlSync(String serverUrl) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SERVER_URL, serverUrl);
        editor.commit();
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    public String getPassword() {
        return preferences.getString(KEY_PASSWORD, null);
    }

    public void setCredentialsSync(String username, String password) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.commit();
    }

    public void setCredentialsAsync(final String username, final String password) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                setCredentialsSync(username, password);
                return null;
            }

        }.execute();
    }
}
