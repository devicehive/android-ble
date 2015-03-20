package com.dataart.btle_android.btle_gateway;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.dataart.android.devicehive.Command;
import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by idyuzheva
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    public final static String ARG_CANCEL_SERVICE = BluetoothLeService.class.getSimpleName();

    private final ConcurrentLinkedQueue<Integer> startIdQueue = new ConcurrentLinkedQueue<Integer>();
    private ExecutorService executor;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
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
        Log.d(TAG, "onCreate");
        executor = Executors.newCachedThreadPool();

        final BTLEApplication app = (BTLEApplication) getApplication();
        deviceHive = app.getDevice();

        mBluetoothServer = new BluetoothServer();
        gateway = new BTLEGateway(mBluetoothServer);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        startIdQueue.add(startId);
        final LeRunnable leRunnable = new LeRunnable();
        executor.execute(leRunnable);
        //setNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        deviceHive.removeCommandListener(commandListener);
        deviceHive.stopProcessingCommands();
        executor.shutdown();
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        // Checks if Bluetooth is supported on the device
        mBluetoothAdapter = mBluetoothManager.getAdapter();
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

    /*void setNotification() {
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.notification);

        //notificationView.setOnClickPendingIntent();

        final Intent intent = new Intent(this, BluetoothLeService.class);
        intent.putExtra(BluetoothLeService.ARG_CANCEL_SERVICE, true);
        //final PendingIntent pendingIntent = PendingIntent.getService()

        final Notification notification = new Notification.Builder(this)
                .setContentTitle("DeviceHive")
                .setContentText("Bluetooth LE is working...")
                .setContent(notificationView)
                .setSmallIcon(R.drawable.ic_stat_ic_launcher)
                .setOngoing(true)
                        //.setContentIntent(intent)
                        //.setWhen(System.currentTimeMillis())
                        //.setAutoCancel(false)
                .build();
        final PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        mNotificationManager.notify(1, notification);
    }*/


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
