package com.dataart.btle_android.btle_gateway;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
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
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.server.BluetoothServer;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;
import com.github.devicehive.client.service.DeviceCommand;
import timber.log.Timber;

/**
 * Created by idyuzheva
 */
public class BluetoothLeService extends Service {

  private final static String TAG = BluetoothLeService.class.getSimpleName();

  public final static String ACTION_BT_PERMISSION_REQUEST =
      TAG.concat("ACTION_BT_PERMISSION_REQUEST");

  private final static int LE_NOTIFICATION_ID = 1;
  private final static String LE_NOTIFICATION_NAME = "DeviceHive";
  private final static String LE_NOTIFICATION_CHANNEL_ID = "devicehive";

  private NotificationManager mNotificationManager;
  private NotificationCompat.Builder mBuilder;

  private BroadcastReceiver mReceiver;

  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothServer mBluetoothServer;
  private BTLEDeviceHive mDeviceHive;
  private BTLEGateway mGateway;
  private LocationManager mLocationManager;

  public BluetoothLeService() {
    super();
  }

  public static void start(final Context context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      context.startForegroundService(new Intent(context, BluetoothLeService.class));
    } else {
      context.startService(new Intent(context, BluetoothLeService.class));
    }
  }

  public static void stop(final Context context) {
    context.stopService(new Intent(context, BluetoothLeService.class));
  }

  @Override public void onCreate() {
    super.onCreate();
    Timber.d("onCreate");
    final BluetoothManager mBluetoothManager =
        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    assert mBluetoothManager != null;
    mBluetoothAdapter = mBluetoothManager.getAdapter();
    if (!mBluetoothAdapter.isEnabled()) {
      send(ACTION_BT_PERMISSION_REQUEST);
    }
    mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mDeviceHive = BTLEDeviceHive.newInstance();
    mBluetoothServer = new BluetoothServer(getApplicationContext());
    mGateway = new BTLEGateway(mBluetoothServer);
    registerReceiver(getBtStateReceiver(), new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

    mDeviceHive.setDeviceListener(device -> mGateway.setDhDevice(device));
    mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        
      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1000,
          new LocationListener() {
            @Override public void onLocationChanged(Location location) {
              mGateway.setLocation(location);
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override public void onProviderEnabled(String provider) {

            }

            @Override public void onProviderDisabled(String provider) {

            }
          });
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    Timber.d("Service.onStartCommand");
    if (mBluetoothAdapter.isEnabled()) {
      mBluetoothServer.scanStart();
    }

    mDeviceHive.setCommandListener(commandListener);

    mDeviceHive.registerDevice();

    Notification notification = prepareNotification();

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      startForeground(LE_NOTIFICATION_ID, notification);
    } else {
      mNotificationManager.notify(LE_NOTIFICATION_ID, mBuilder.build());
    }
    return START_NOT_STICKY;
  }

  @Override public void onDestroy() {
    Timber.d("Service.onDestroy");
    mDeviceHive.removeCommandListener();

    mDeviceHive.disconnect();

    if (mReceiver != null) {
      unregisterReceiver(mReceiver);
    }
    mNotificationManager.cancel(LE_NOTIFICATION_ID);
    super.onDestroy();
    Timber.d("BluetoothLeService was destroyed");
  }

  private final BTLEDeviceHive.CommandListener commandListener =
      new BTLEDeviceHive.CommandListener() {
        @Override public void onDeviceReceivedCommand(DeviceCommand command) {
          Timber.d("Device received Command in BluetoothLeService");
          mGateway.doCommand(getApplicationContext(), command);
        }
      };

  @Override public IBinder onBind(Intent intent) {
    Timber.d("Service.onBind");
    return null;
  }

  @Override public boolean onUnbind(Intent intent) {
    Timber.d("Service.onUnbind");
    return super.onUnbind(intent);
  }

  private void notifyNewState(final String text) {
    Timber.d("Service.onUnbind");
    mBuilder.setContentText(text);
    mNotificationManager.notify(LE_NOTIFICATION_ID, mBuilder.build());
  }

  private BroadcastReceiver getBtStateReceiver() {
    if (mReceiver == null) {
      mReceiver = new BluetoothStateReceiver() {
        @Override protected void onBluetoothOff() {
          notifyNewState(getString(R.string.notification_bt_off));
          Toast.makeText(BluetoothLeService.this, getString(R.string.notification_bt_off),
              Toast.LENGTH_LONG).show();
        }

        @Override protected void onBluetoothOn() {
          mBluetoothServer.scanStart();
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

  private Notification prepareNotification() {
    final Intent resultIntent = new Intent(this, MainActivity.class);
    final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addParentStack(MainActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    final PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel(LE_NOTIFICATION_CHANNEL_ID, LE_NOTIFICATION_NAME,
              NotificationManager.IMPORTANCE_HIGH);
      channel.enableVibration(false);
      channel.setShowBadge(true);
      mNotificationManager.createNotificationChannel(channel);
    }

    mBuilder = new NotificationCompat.Builder(this, LE_NOTIFICATION_CHANNEL_ID).setContentText(
        getString(R.string.notification_bt_on))
        .setContentTitle(getString(R.string.device_hive))
        .setSmallIcon(R.drawable.ic_le_service)
        .setAutoCancel(false)
        .setOngoing(true)
        .setContentIntent(resultPendingIntent);

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      mBuilder.setChannelId(LE_NOTIFICATION_CHANNEL_ID);
    }

    return mBuilder.build();
  }
}
