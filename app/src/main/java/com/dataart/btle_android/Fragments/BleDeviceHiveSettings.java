package com.dataart.btle_android.Fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.BluetoothLeService;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

/**
 * Created by idyuzheva
 */

public class BleDeviceHiveSettings extends Fragment {

    private EditText serverUrlEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private TextView hintText;
    private Button serviceButton;
    private Button restartServiceButton;

    private BTLEDevicePreferences prefs;

    private boolean isServiceStarted;

    public BleDeviceHiveSettings() {
    }

    public static Fragment newInstance() {
        return new BleDeviceHiveSettings();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.SECTION_GATEWAY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_settings, container, false);
        prefs = new BTLEDevicePreferences();

        serverUrlEditText = (EditText) rootView.findViewById(R.id.server_url_edit);
        usernameEditText = (EditText) rootView.findViewById(R.id.username_edit);
        passwordEditText = (EditText) rootView.findViewById(R.id.password_edit);
        hintText = (TextView) rootView.findViewById(R.id.hintText);

        resetValues();

        serviceButton = (Button) rootView.findViewById(R.id.service_button);
        serviceButton.setOnClickListener(serviceClickListener);

        restartServiceButton = (Button) rootView.findViewById(R.id.save_button);
        restartServiceButton.setOnClickListener(restartClickListener);

        serverUrlEditText.setOnEditorActionListener(changeListener);
        serverUrlEditText.addTextChangedListener(changeWatcher);

        usernameEditText.setOnEditorActionListener(changeListener);
        usernameEditText.addTextChangedListener(changeWatcher);

        passwordEditText.setOnEditorActionListener(changeListener);
        passwordEditText.addTextChangedListener(changeWatcher);

        if (isLeServiceRunning()) {
            onServiceRunning();
        }

        return rootView;
    }

    private boolean isLeServiceRunning() {
        final ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BluetoothLeService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private final View.OnClickListener serviceClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!isServiceStarted) {
                saveValues();
                onServiceRunning();
                BluetoothLeService.start(getActivity());
            } else {
                onServiceStopped();
                BluetoothLeService.stop(getActivity());
            }
        }
    };

    private void onServiceRunning() {
        isServiceStarted = true;
        serviceButton.setText("Stop Service");
    }

    private void onServiceStopped() {
        isServiceStarted = false;
        serviceButton.setText("Start Service");
    }

    private final View.OnClickListener restartClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            saveValues();
            BluetoothLeService.stop(getActivity());
            BluetoothLeService.start(getActivity());
            restartServiceButton.setVisibility(View.GONE);
            hintText.setVisibility(View.GONE);
        }
    };

    private boolean isRestartRequired() {
        final String newUrl = serverUrlEditText.getText().toString();
        final String newUserName = usernameEditText.getText().toString();
        final String newPassword = passwordEditText.getText().toString();
        return !(prefs.getServerUrl().equals(newUrl) &&
                prefs.getUsername().equals(newUserName) &&
                prefs.getPassword().equals(newPassword));
    }

    private void onDataChanged() {
        if (isServiceStarted && isRestartRequired()) {
            hintText.setVisibility(View.VISIBLE);
            restartServiceButton.setVisibility(View.VISIBLE);
            serviceButton.setVisibility(View.GONE);
        } else {
            hintText.setVisibility(View.GONE);
            restartServiceButton.setVisibility(View.GONE);
            serviceButton.setVisibility(View.VISIBLE);
        }
    }

    private void resetValues() {
        serverUrlEditText.setText(prefs.getServerUrl());
        usernameEditText.setText(prefs.getUsername());
        passwordEditText.setText(prefs.getPassword());
    }

    private void saveValues() {
        final String serverUrl = serverUrlEditText.getText().toString();
        final String username = usernameEditText.getText().toString();
        final String password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(serverUrl)) {
            serverUrlEditText.setError(getString(R.string.error_message_empty_server_url));
        } else if (TextUtils.isEmpty(username)) {
            usernameEditText.setError(getString(R.string.error_message_empty_username));
        } else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_message_empty_password));
        } else {
            prefs.setCredentialsSync(username, password);
            prefs.setServerUrlSync(serverUrl);
        }
    }

    private final TextView.OnEditorActionListener changeListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                onDataChanged();
            }
            return false;
        }
    };

    private final TextWatcher changeWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            onDataChanged();
        }
    };

}