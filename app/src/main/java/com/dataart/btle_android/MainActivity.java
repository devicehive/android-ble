package com.dataart.btle_android;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dataart.android.devicehive.Notification;
import com.dataart.btle_android.btle_gateway.BluetoothLeService;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;


public class MainActivity extends Activity implements BTLEDeviceHive.NotificationListener {

    private static final String TAG = MainActivity.class.getName();

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private EditText serverUrlEditText;
    private EditText gatewayIdEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private TextView hintText;
    private Button serviceButton;
    private Button restartServiceButton;

    private BTLEDevicePreferences prefs;

    private boolean isServiceStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (getActionBar() != null) {
            getActionBar().setTitle(R.string.app_name);
        }

        if (!isBluetoothLeSupported()) {
            Toast.makeText(this, R.string.error_message_btle_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!isBluetoothSupported()) {
            Toast.makeText(this, R.string.error_message_bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        prefs = new BTLEDevicePreferences();

        serverUrlEditText = (EditText) findViewById(R.id.server_url_edit);
        gatewayIdEditText = (EditText) findViewById(R.id.settings_gateway_id);
        usernameEditText = (EditText) findViewById(R.id.username_edit);
        passwordEditText = (EditText) findViewById(R.id.password_edit);
        hintText = (TextView) findViewById(R.id.hintText);

        resetValues();

        serviceButton = (Button) findViewById(R.id.service_button);
        serviceButton.setOnClickListener(serviceClickListener);

        restartServiceButton = (Button) findViewById(R.id.save_button);
        restartServiceButton.setOnClickListener(restartClickListener);

        serverUrlEditText.setOnEditorActionListener(changeListener);
        serverUrlEditText.addTextChangedListener(changeWatcher);

        gatewayIdEditText.setOnEditorActionListener(changeListener);
        gatewayIdEditText.addTextChangedListener(changeWatcher);

        usernameEditText.setOnEditorActionListener(changeListener);
        usernameEditText.addTextChangedListener(changeWatcher);

        passwordEditText.setOnEditorActionListener(changeListener);
        passwordEditText.addTextChangedListener(changeWatcher);

        if (isLeServiceRunning()) {
            onServiceRunning();
        }
    }

    private boolean isBluetoothLeSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isBluetoothSupported() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter");
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isLeServiceRunning() {
        final ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
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
                BluetoothLeService.start(MainActivity.this);
            } else {
                onServiceStopped();
                BluetoothLeService.stop(MainActivity.this);
            }
        }
    };

    private void onServiceRunning() {
        isServiceStarted = true;
        serviceButton.setText(R.string.button_stop);
    }

    private void onServiceStopped() {
        isServiceStarted = false;
        serviceButton.setText(R.string.button_start);
    }

    private final View.OnClickListener restartClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            saveValues();
            BluetoothLeService.stop(MainActivity.this);
            BluetoothLeService.start(MainActivity.this);
            restartServiceButton.setVisibility(View.GONE);
            serviceButton.setVisibility(View.VISIBLE);
            onServiceRunning();
            hintText.setVisibility(View.GONE);
        }
    };

    private boolean isRestartRequired() {
        final String newUrl = serverUrlEditText.getText().toString();
        final String newGatewayId = gatewayIdEditText.getText().toString();
        final String newUserName = usernameEditText.getText().toString();
        final String newPassword = passwordEditText.getText().toString();
        return !(prefs.getServerUrl().equals(newUrl) &&
                prefs.getGatewayId().equals(newGatewayId) &&
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
        gatewayIdEditText.setText(TextUtils.isEmpty(prefs.getGatewayId()) ?
                getString(R.string.default_gateway_id) : prefs.getGatewayId());
        usernameEditText.setText(prefs.getUsername());
        passwordEditText.setText(prefs.getPassword());
    }

    private void saveValues() {
        final String serverUrl = serverUrlEditText.getText().toString();
        final String gatewayId = gatewayIdEditText.getText().toString();
        final String username = usernameEditText.getText().toString();
        final String password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(serverUrl)) {
            serverUrlEditText.setError(getString(R.string.error_message_empty_server_url));
        } else if (TextUtils.isEmpty(gatewayId)) {
            gatewayIdEditText.setError(getString(R.string.error_message_empty_gateway_id));
        } else if (TextUtils.isEmpty(username)) {
            usernameEditText.setError(getString(R.string.error_message_empty_username));
        } else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_message_empty_password));
        } else {
            prefs.setCredentialsSync(username, password);
            prefs.setServerUrlSync(serverUrl);
            prefs.setGatewayIdSync(gatewayId);
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

    @Override
    public void onDeviceSentNotification(Notification notification) {

    }

    @Override
    public void onDeviceFailedToSendNotification(Notification notification) {

    }

}
