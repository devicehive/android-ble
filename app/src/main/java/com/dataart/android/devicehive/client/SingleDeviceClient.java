package com.dataart.android.devicehive.client;

import android.content.Context;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.client.commands.NotificationsRetrivalCommand;
import com.dataart.android.devicehive.client.commands.PollDeviceNotificationsCommand;

/**
 * Represents a single device client which provides high-level API for
 * communication with particular device. This class is abstract and designed to
 * be subclassed in order to handle incoming notifications. Also this class
 * provides a number of various callbacks:
 * {@link #onStartReceivingNotifications()},
 * {@link #onStopReceivingNotifications()},
 * {@link #onStartSendingCommand(Command)},
 * {@link #onFinishSendingCommand(Command)},
 * {@link #onFailSendingCommand(Command)}, etc.
 * 
 */
public abstract class SingleDeviceClient extends DeviceClient {

	private DeviceData device;

	/**
	 * Construct client with given {@link android.content.Context} and {@link DeviceData}
	 * objects.
	 * 
	 * @param context
	 *            {@link android.content.Context} object. In most cases this should be
	 *            application context which stays alive during the entire life
	 *            of an application.
	 * @param deviceData
	 *            {@link DeviceData} object which describes device to
	 *            communicate with.
	 */
	public SingleDeviceClient(Context context, final DeviceData device) {
		super(context);
		this.device = device;
		setServiceConnection(new SingleDeviceClientServiceConnection(context));
	}

	private class SingleDeviceClientServiceConnection extends
			ClientServiceConnection {

		public SingleDeviceClientServiceConnection(Context context) {
			super(context);
		}

		@Override
		protected NotificationsRetrivalCommand getPollNotificationsCommand(
				String lastNotificationPollTimestamp, Integer waitTimeout) {
			return new PollDeviceNotificationsCommand(device.getId(),
					lastNotificationPollTimestamp, waitTimeout);
		}

		@Override
		protected void didReceiveNotification(Notification notification) {
			onReceiveNotification(notification);
		}
	}

	/**
	 * Get corresponding device.
	 * 
	 * @return {@link DeviceData} object.
	 */
	public DeviceData getDevice() {
		return device;
	}

	/**
	 * Send command to the device.
	 * 
	 * @param command
	 *            {@link Command} to be sent.
	 */
	public void sendCommand(final Command command) {
		serviceConnection.sendCommand(device, command);
	}

	/**
	 * Reload device data. Current device data is updated with instance of
	 * {@link DeviceData} retrieved from the server.
	 * 
	 * @see #onFinishReloadingDeviceData(DeviceData)
	 * @see #onFailReloadingDeviceData()
	 */
	public void reloadDeviceData() {
		serviceConnection.reloadDeviceData(device);
	}

	/* package */void onReloadDeviceDataFinishedInternal(DeviceData deviceData) {
		this.device = deviceData;
		onFinishReloadingDeviceData(deviceData);
	}

	/**
	 * Handle received notification. Can be called either on main (UI) thread or
	 * some background thread depending on
	 * {@link #shouldReceiveNotificationAsynchronously(Notification)} method
	 * return value.
	 * 
	 * @param notification
	 *            {@link Notification} instance to handle by the client.
	 */
	protected abstract void onReceiveNotification(
			final Notification notification);
}
