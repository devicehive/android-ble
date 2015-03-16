package com.dataart.btle_android.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

/**
 * Created by idyuzheva
 */

public class BleDeviceHiveSettings extends Fragment {

    private EditText serverUrlEdit;
    private EditText usernameEdit;
    private EditText passwordEdit;

    private BTLEDevicePreferences prefs;

    private SettingsChangesListener listener;

    public BleDeviceHiveSettings() {
    }

    public static Fragment newInstance(final SettingsChangesListener listener) {
        final BleDeviceHiveSettings fragment = new BleDeviceHiveSettings();
        fragment.setChangesListener(listener);
        return fragment;
    }

    public void setChangesListener(final SettingsChangesListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_settings, container, false);

        serverUrlEdit = (EditText) rootView.findViewById(R.id.server_url_edit);
        usernameEdit = (EditText) rootView.findViewById(R.id.username_edit);
        passwordEdit = (EditText) rootView.findViewById(R.id.password_edit);

        prefs = new BTLEDevicePreferences();

        rootView.findViewById(R.id.undo_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetValues();
                    }
                });
        rootView.findViewById(R.id.save_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveValues();
                    }
                });
        resetValues();

        return rootView;
    }

    private void resetValues() {
        serverUrlEdit.setText(prefs.getServerUrl());
        usernameEdit.setText(prefs.getUsername());
        passwordEdit.setText(prefs.getPassword());
        setEnpointUrl(prefs.getServerUrl());
    }

    private void saveValues() {
        final String serverUrl = serverUrlEdit.getText().toString();
        final String username = usernameEdit.getText().toString();
        final String password = passwordEdit.getText().toString();
        if (TextUtils.isEmpty(serverUrl)) {
            serverUrlEdit.setError(getString(R.string.error_message_empty_server_url));
        } else if (TextUtils.isEmpty(username)) {
            usernameEdit.setError(getString(R.string.error_message_empty_username));
        } else if (TextUtils.isEmpty(password)) {
            passwordEdit.setError(getString(R.string.error_message_empty_password));
        } else {
            prefs.setCredentialsSync(username, password);
            prefs.setServerUrlSync(serverUrl);
            setEnpointUrl(serverUrl);
            Toast.makeText(getActivity(), R.string.info_message_save_settings, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.SECTION_DEVICE_HIVE_SETTINGS);

    }

    private void setEnpointUrl(final String apiEndPointUrl) {
        if (listener != null) {
            listener.onApiEnpointUrlChanged(apiEndPointUrl);
        }
    }

    public interface SettingsChangesListener {
        void onApiEnpointUrlChanged(final String apiEndPointUrl);
    }

}