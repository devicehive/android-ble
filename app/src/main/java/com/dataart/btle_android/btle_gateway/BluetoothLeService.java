package com.dataart.btle_android.btle_gateway;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.dataart.android.devicehive.Command;
import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by idyuzheva
 */
public class BluetoothLeService extends Service {

    private final static int LE_NOTIFICATION_ID = 1;

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private final ConcurrentLinkedQueue<Integer> startIdQueue = new ConcurrentLinkedQueue<Integer>();
    private ExecutorService executor;

    private NotificationManager mNotificationManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothServer mBluetoothServer;

    private BTLEDeviceHive deviceHive;
    private BTLEGateway gateway;

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
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        executor = Executors.newCachedThreadPool();

        final BTLEApplication app = (BTLEApplication) getApplication();
        deviceHive = app.getDevice();

        mBluetoothServer = new BluetoothServer();
        gateway = new BTLEGateway(mBluetoothServer);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startIdQueue.add(startId);
        final LeRunnable leRunnable = new LeRunnable();
        executor.execute(leRunnable);
        setNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        deviceHive.removeCommandListener(commandListener);
        deviceHive.stopProcessingCommands();
        executor.shutdown();
        mNotificationManager.cancel(LE_NOTIFICATION_ID);
        super.onDestroy();
        Log.d(TAG, "BluetoothLeService was destroyed.");
    }

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    private final BTLEDeviceHive.CommandListener commandListener = new BTLEDeviceHive.CommandListener() {
        @Override
        public void onDeviceReceivedCommand(Command command) {
            Log.d(TAG, "onDeviceReceivedCommand");
            gateway.doCommand(getApplicationContext(), deviceHive, command);
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

    private void setNotification() {
        final Intent resultIntent = new Intent(this, MainActivity.class);
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        final PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = new Notification.Builder(this)
                .setContentTitle("DeviceHive")
                .setContentText("Bluetooth LE is working...")
                .setSmallIcon(R.drawable.ic_le_service)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
                .build();
        mNotificationManager.notify(LE_NOTIFICATION_ID, notification);
    }

    private class LeRunnable implements Runnable {

        public void run() {
            mBluetoothServer.scanStart(BluetoothLeService.this);

            final BTLEDevicePreferences prefs = new BTLEDevicePreferences();
            deviceHive.setApiEnpointUrl(prefs.getServerUrl());

            deviceHive.addCommandListener(commandListener);
            if (!deviceHive.isRegistered()) {
                deviceHive.registerDevice();
            }
            deviceHive.startProcessingCommands();
        }
    }

}
