package com.dataart.btle_android.helpers.ble.base;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import com.dataart.btle_android.R;

import timber.log.Timber;

/**
 * Created by Constantine Mars on 12/13/15.
 * <p>
 * Base BLE init helper logic, common for all Android versions, starting with JellyBean MR2
 */
public abstract class BleInitializer {
    private static final int REQUEST_ENABLE_BT = 3001;

    protected final Activity activity;
    protected final InitCompletionCallback initCompletionCallback;
    protected BluetoothAdapter bluetoothAdapter;

    public BleInitializer(Activity activity, InitCompletionCallback initCompletionCallback) {
        this.activity = activity;
        this.initCompletionCallback = initCompletionCallback;
    }


    /**
     * Enabling Bluetooth should be always the last step of initialization,
     * after which initCompletionCallback will be called
     */
    private boolean declined = false;

    /**
     * Init delivers BluetoothAdapter for all platform-specific versions of initializer
     */
    protected void init() {
        final BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    /**
     * Start must be called before beginning scanning or any other opeartions on bluetooth
     */
    public abstract void start();

    public boolean enableBluetooth() {
        if (bluetoothAdapter == null) {
            Timber.e(activity.getString(R.string.bt_not_supported));
            return false;
        }

        if (!bluetoothAdapter.isEnabled() && !declined) {
            Timber.i(activity.getString(R.string.bt_enabling_started));

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }

        Timber.i(activity.getString(R.string.bt_enabled));
        initCompletionCallback.onInitCompleted(bluetoothAdapter);
        return true;
    }

    /**
     * Need to be called from Activity onActivityResult
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    start();
                } else {
                    declined = false;
                }
                break;
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    }

    public void onStart() {
    }

    public void onStop() {
    }

    public void onResume() {
    }


    public interface InitCompletionCallback {
        void onInitCompleted(BluetoothAdapter bluetoothAdapter);
    }
}
