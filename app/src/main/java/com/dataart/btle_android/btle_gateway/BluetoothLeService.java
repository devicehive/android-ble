package com.dataart.btle_android.btle_gateway;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.dataart.android.devicehive.Command;
import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by idyuzheva
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private final ConcurrentLinkedQueue<Integer> startIdQueue = new ConcurrentLinkedQueue<Integer>();
    private final IBinder mBinder = new LocalBinder();
    private ExecutorService executor;

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
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        //return mBinder;
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        // close();
        return super.onUnbind(intent);
    }

    public void onDestroy() {
        deviceHive.removeCommandListener(commandListener);
        deviceHive.stopProcessingCommands();
        executor.shutdown();
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        startIdQueue.add(startId);
        myRunnable myRunnable = new myRunnable();
        executor.execute(myRunnable);
        return START_STICKY;
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        executor = Executors.newCachedThreadPool();
    }

    private final BTLEDeviceHive.CommandListener commandListener = new BTLEDeviceHive.CommandListener() {
        @Override
        public void onDeviceReceivedCommand(Command command) {
            Log.d(TAG, "onDeviceReceivedCommand");
            gateway.doCommand(getApplicationContext(), deviceHive, command);
        }
    };

    private class myRunnable implements Runnable {

        public void run() {
            gateway = new BTLEGateway(new BluetoothServer());
            final BTLEApplication app = (BTLEApplication) getApplication();
            deviceHive = app.getDevice();
            deviceHive.addCommandListener(commandListener);
            if (!deviceHive.isRegistered()) {
                deviceHive.registerDevice();
            }
            deviceHive.startProcessingCommands();
        }
    }

    private class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

}
