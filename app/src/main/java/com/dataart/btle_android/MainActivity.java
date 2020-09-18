package com.dataart.btle_android;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.dataart.btle_android.btle_gateway.BluetoothLeService;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;
import com.dataart.btle_android.helpers.BleHelpersFactory;
import com.dataart.btle_android.helpers.ble.base.BleInitializer;
import java.util.Objects;
import java.util.UUID;
import timber.log.Timber;


public class MainActivity extends AppCompatActivity {

    private BleInitializer bleInitializer;

    private BluetoothManager mBluetoothManager;
    private EditText serverUrlEditText;
    private EditText gatewayIdEditText;
    private EditText refreshTokenEditText;
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
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
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

        prefs = BTLEDevicePreferences.getInstance();

        serverUrlEditText = findViewById(R.id.server_url_edit);
        gatewayIdEditText = findViewById(R.id.settings_gateway_id);
        refreshTokenEditText = findViewById(R.id.refresh_token_edit);
        hintText = findViewById(R.id.hintText);

        resetValues();

        serviceButton = findViewById(R.id.service_button);
        //noinspection ConstantConditions
        serviceButton.setOnClickListener(v -> {
            Timber.d(String.valueOf(validateValues()));
            if (validateValues()) {
                bleInitializer.start();
            }
        });

        restartServiceButton = findViewById(R.id.save_button);
        //noinspection ConstantConditions
        restartServiceButton.setOnClickListener(restartClickListener);

        serverUrlEditText.setOnEditorActionListener(changeListener);
        serverUrlEditText.addTextChangedListener(changeWatcher);

        gatewayIdEditText.setOnEditorActionListener(changeListener);
        gatewayIdEditText.addTextChangedListener(changeWatcher);

        refreshTokenEditText.setOnEditorActionListener(changeListener);
        refreshTokenEditText.addTextChangedListener(changeWatcher);

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
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (Objects.equals(BluetoothLeService.class.getName(), service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService() {
        Timber.d("HERE %s", isServiceStarted);
        if (!isServiceStarted) {
            Timber.d("Started");
            saveValues();
            onServiceRunning();
            BluetoothLeService.start(MainActivity.this);
        } else {
            Timber.d("Not Started");
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
        String newUrl = serverUrlEditText.getText().toString();
        String newGatewayId = gatewayIdEditText.getText().toString();
        String newRefreshToken = refreshTokenEditText.getText().toString();

        return !(Objects.equals(prefs.getServerUrl(), newUrl) &&
                Objects.equals(prefs.getGatewayId(), newGatewayId) &&
                Objects.equals(prefs.getRefreshToken(), newRefreshToken));
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
                        ? getString(R.string.default_gateway_id) + "-" +
                            UUID.randomUUID().toString().substring(0, 4)
                        : gatewayId
        );

        String refreshToken = prefs.getRefreshToken();
        refreshTokenEditText.setText(
                TextUtils.isEmpty(refreshToken)
                        ? ""
                        : refreshToken
        );
    }

    private void resetErrors() {
        serverUrlEditText.setError(null);
        gatewayIdEditText.setError(null);
        refreshTokenEditText.setError(null);
    }

    private boolean validateValues() {
        resetErrors();

        final String serverUrl = serverUrlEditText.getText().toString();
        final String gatewayId = gatewayIdEditText.getText().toString();
        final String refreshToken = refreshTokenEditText.getText().toString();

        if (TextUtils.isEmpty(serverUrl)) {
            serverUrlEditText.setError(getString(R.string.error_message_empty_server_url));
            serverUrlEditText.requestFocus();
        } else if (TextUtils.isEmpty(gatewayId)) {
            gatewayIdEditText.setError(getString(R.string.error_message_empty_gateway_id));
            gatewayIdEditText.requestFocus();
        } else if (TextUtils.isEmpty(refreshToken)) {
            refreshTokenEditText.setError(getString(R.string.error_message_empty_refresh_token));
            refreshTokenEditText.requestFocus();
        } else {
            return true;
        }

        return false;
    }

    private void saveValues() {
        String serverUrl = serverUrlEditText.getText().toString().trim();
        String gatewayId = gatewayIdEditText.getText().toString().trim();
        String refreshToken = refreshTokenEditText.getText().toString().trim();

        prefs.setRefreshTokenSync(refreshToken);
        prefs.setServerUrlSync(serverUrl);
        prefs.setGatewayIdSync(gatewayId);
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
}
