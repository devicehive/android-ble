package com.dataart.btle_android.btle_gateway;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.network.DeviceHiveApiService;
import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.future.SimpleCallableFuture;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

/**
 * Created by idyuzheva
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    public final static String ACTION_BT_PERMISSION_REQUEST = TAG
            .concat("ACTION_BT_PERMISSION_REQUEST");

    private final static int LE_NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private BroadcastReceiver mReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServer mBluetoothServer;
    private BTLEDeviceHive mDeviceHive;
    private BTLEGateway mGateway;

    public BluetoothLeService() {
        super();
    }

    public static final void start(final Context context) {
        context.startService(new Intent(context, BluetoothLeService.class));
    }

    public static final void stop(final Context context) {
        context.stopService(new Intent(context, BluetoothLeService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            send(ACTION_BT_PERMISSION_REQUEST);
        }
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mDeviceHive = BTLEDeviceHive.newInstance(this);
        mBluetoothServer = new BluetoothServer(getApplicationContext());
        mGateway = new BTLEGateway(mBluetoothServer);
        registerReceiver(getBtStateReceiver(), new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothServer.scanStart(BluetoothLeService.this);
        }

        final BTLEDevicePreferences prefs = new BTLEDevicePreferences();
        mDeviceHive.setApiEnpointUrl(prefs.getServerUrl());

//        FIXME: originally there can be multiple command listeners - I can't understand why we need multiple, so replace it with single
//        mDeviceHive.addCommandListener(commandListener);
        mDeviceHive.setCommandListener(commandListener);
        if (!mDeviceHive.isRegistered()) {
            mDeviceHive.registerDevice();
        }
        mDeviceHive.startProcessingCommands();
        setNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mDeviceHive.removeCommandListener();
        mDeviceHive.stopProcessingCommands();
        stopService(new Intent(this, DeviceHiveApiService.class));
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        mNotificationManager.cancel(LE_NOTIFICATION_ID);
        super.onDestroy();
        Log.d(TAG, "BluetoothLeService was destroyed");
    }

    private final BTLEDeviceHive.CommandListener commandListener = new BTLEDeviceHive.CommandListener() {
        @Override
        public SimpleCallableFuture<CommandResult> onDeviceReceivedCommand(Command command) {
            Log.d(TAG, "Device received Command in BluetoothLeService");
            return mGateway.doCommand(getApplicationContext(), mDeviceHive, command);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void notifyNewState(final String text) {
        mBuilder.setContentText(text);
        mNotificationManager.notify(LE_NOTIFICATION_ID, mBuilder.build());
    }

    private BroadcastReceiver getBtStateReceiver() {
        if (mReceiver == null) {
            mReceiver = new BluetoothStateReceiver() {
                @Override
                protected void onBluetoothOff() {
                    notifyNewState(getString(R.string.notification_bt_off));
                    Toast.makeText(BluetoothLeService.this, getString(R.string.notification_bt_off),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                protected void onBluetoothOn() {
                    mBluetoothServer.scanStart(BluetoothLeService.this);
                    notifyNewState(getString(R.string.notification_bt_on));
                }
            };
        }
        return mReceiver;
    }

    private void send(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void setNotification() {
        final Intent resultIntent = new Intent(this, MainActivity.class);
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        final PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentText(getString(R.string.notification_bt_on))
                .setContentTitle(getString(R.string.device_hive))
                .setSmallIcon(R.drawable.ic_le_service)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(resultPendingIntent);

        mNotificationManager.notify(LE_NOTIFICATION_ID, mBuilder.build());
    }

}
