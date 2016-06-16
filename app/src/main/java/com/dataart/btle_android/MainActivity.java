package com.dataart.btle_android;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dataart.android.devicehive.Notification;
import com.dataart.btle_android.btle_gateway.BluetoothLeService;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;
import com.dataart.btle_android.helpers.BleHelpersFactory;
import com.dataart.btle_android.helpers.ble.base.BleInitializer;

import timber.log.Timber;


public class MainActivity extends AppCompatActivity implements BTLEDeviceHive.NotificationListener {

    private BleInitializer bleInitializer;

    private BluetoothManager mBluetoothManager;
    private EditText serverUrlEditText;
    private EditText gatewayIdEditText;
    private EditText accessKeyEditText;
    private TextView hintText;
    private Button serviceButton;
    private Button restartServiceButton;
    private BTLEDevicePreferences prefs;
    private boolean isServiceStarted;
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
    private final TextView.OnEditorActionListener changeListener = (textView, actionId, keyEvent) -> {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
            onDataChanged();
        }
        return false;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        Timber.plant(new Timber.DebugTree());

//        Warn if developer tries to lower SDK version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            alertSdkVersionMismatch(() -> {
                finish();
                System.exit(0);
            });

            return;
        }

//        BleInitializer will start service on initialization success
        bleInitializer = BleHelpersFactory.getInitializer(this, bluetoothAdapter -> startService());

        init();
    }

    private void fatalDialog(int message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.unsupported)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .create().show();
    }

    private void init() {

        if (!isBluetoothLeSupported()) {
            fatalDialog(R.string.error_message_btle_not_supported);
            return;
        }
        if (!isBluetoothSupported()) {
            fatalDialog(R.string.error_message_bt_not_supported);
            return;
        }

        prefs = new BTLEDevicePreferences();

        serverUrlEditText = (EditText) findViewById(R.id.server_url_edit);
        gatewayIdEditText = (EditText) findViewById(R.id.settings_gateway_id);
        accessKeyEditText = (EditText) findViewById(R.id.accesskey_edit);
        hintText = (TextView) findViewById(R.id.hintText);

        resetValues();

        serviceButton = (Button) findViewById(R.id.service_button);
        //noinspection ConstantConditions
        serviceButton.setOnClickListener(v -> {
            if (validateValues()) {
                bleInitializer.start();
            }
        });

        restartServiceButton = (Button) findViewById(R.id.save_button);
        //noinspection ConstantConditions
        restartServiceButton.setOnClickListener(restartClickListener);

        serverUrlEditText.setOnEditorActionListener(changeListener);
        serverUrlEditText.addTextChangedListener(changeWatcher);

        gatewayIdEditText.setOnEditorActionListener(changeListener);
        gatewayIdEditText.addTextChangedListener(changeWatcher);

        accessKeyEditText.setOnEditorActionListener(changeListener);
        accessKeyEditText.addTextChangedListener(changeWatcher);

        if (isLeServiceRunning()) {
            onServiceRunning();
        }
    }

    private boolean isBluetoothLeSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private boolean isBluetoothSupported() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Timber.e(getString(R.string.bt_unable_init));
                return false;
            }
        }
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Timber.e(getString(R.string.bt_unable_get_btm));
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bleInitializer.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        bleInitializer.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        bleInitializer.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    private void startService() {
        if (!isServiceStarted) {
            saveValues();
            onServiceRunning();
            BluetoothLeService.start(MainActivity.this);
        } else {
            onServiceStopped();
            BluetoothLeService.stop(MainActivity.this);
        }
    }

    private void onServiceRunning() {
        isServiceStarted = true;
        serviceButton.setText(R.string.button_stop);
    }

    private void onServiceStopped() {
        isServiceStarted = false;
        serviceButton.setText(R.string.button_start);
    }

    private boolean isRestartRequired() {
        final String newUrl = serverUrlEditText.getText().toString();
        final String newGatewayId = gatewayIdEditText.getText().toString();
        final String newAccessKey = accessKeyEditText.getText().toString();
        return !(prefs.getServerUrl().equals(newUrl) &&
                prefs.getGatewayId().equals(newGatewayId) &&
                prefs.getAccessKey().equals(newAccessKey));
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
        String serverUrl = prefs.getServerUrl();
        serverUrlEditText.setText(
                TextUtils.isEmpty(serverUrl)
                        ? getString(R.string.default_server_url)
                        : serverUrl
        );

        String gatewayId = prefs.getGatewayId();
        gatewayIdEditText.setText(
                TextUtils.isEmpty(gatewayId)
                        ? getString(R.string.default_gateway_id)
                        : gatewayId
        );

        String accessKey = prefs.getAccessKey();
        accessKeyEditText.setText(
                TextUtils.isEmpty(accessKey)
                        ? ""
                        : accessKey
        );
    }

    private void resetErrors() {
        serverUrlEditText.setError(null);
        gatewayIdEditText.setError(null);
        accessKeyEditText.setError(null);
    }

    private boolean validateValues() {
        resetErrors();

        final String serverUrl = serverUrlEditText.getText().toString();
        final String gatewayId = gatewayIdEditText.getText().toString();
        final String accessKey = accessKeyEditText.getText().toString();

        if (TextUtils.isEmpty(serverUrl)) {
            serverUrlEditText.setError(getString(R.string.error_message_empty_server_url));
            serverUrlEditText.requestFocus();
        } else if (TextUtils.isEmpty(gatewayId)) {
            gatewayIdEditText.setError(getString(R.string.error_message_empty_gateway_id));
            gatewayIdEditText.requestFocus();
        } else if (TextUtils.isEmpty(accessKey)) {
            accessKeyEditText.setError(getString(R.string.error_message_empty_accesskey));
            accessKeyEditText.requestFocus();
        } else {
            return true;
        }

        return false;
    }

    private void saveValues() {
        final String serverUrl = serverUrlEditText.getText().toString();
        final String gatewayId = gatewayIdEditText.getText().toString();
        final String accessKey = accessKeyEditText.getText().toString();

        prefs.setAccessKeySync(accessKey);
        prefs.setServerUrlSync(serverUrl);
        prefs.setGatewayIdSync(gatewayId);

    }

    @Override
    public void onDeviceSentNotification(Notification notification) {

    }

    @Override
    public void onDeviceFailedToSendNotification(Notification notification) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        bleInitializer.onStart();
    }

    @Override
    protected void onStop() {
        bleInitializer.onStop();
        super.onStop();
    }

    private void alertSdkVersionMismatch(final Runnable runnable) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sdk_version_warning_title)
                .setMessage(R.string.sdk_version_warning)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> runnable.run())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
