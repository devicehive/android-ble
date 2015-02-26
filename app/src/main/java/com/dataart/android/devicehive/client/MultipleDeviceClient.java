package com.dataart.android.devicehive.client;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.client.commands.NotificationsRetrivalCommand;
import com.dataart.android.devicehive.client.commands.PollMultipleDeviceNotificationsCommand;
import com.dataart.android.devicehive.client.commands.PollMultipleDeviceNotificationsCommand.DeviceNotification;

/**
 * Represents a device client which provides high-level API for communication
 * and receive notifications from several devices. This class is abstract and
 * designed to be subclassed in order to handle incoming notifications. Also
 * this class provides a number of various callbacks:
 * {@link #onStartReceivingNotifications()},
 * {@link #onStopReceivingNotifications()},
 * {@link #onStartSendingCommand(Command)},
 * {@link #onFinishSendingCommand(Command)},
 * {@link #onFailSendingCommand(Command)}, etc.
 * 
 */
public abstract class MultipleDeviceClient extends DeviceClient {

	private List<DeviceData> devices;

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
	public MultipleDeviceClient(Context context, final List<DeviceData> devices) {
		super(context);
		this.devices = devices;
		setServiceConnection(new MultipleDeviceClientServiceConnection(context));
	}

	private class MultipleDeviceClientServiceConnection extends
			ClientServiceConnection {

		public MultipleDeviceClientServiceConnection(Context context) {
			super(context);
		}

		@Override
		protected NotificationsRetrivalCommand getPollNotificationsCommand(
				String lastNotificationPollTimestamp, Integer waitTimeout) {
			return new PollMultipleDeviceNotificationsCommand(getDeviceGuids(),
					lastNotificationPollTimestamp, waitTimeout);
		}

		@Override
		protected void didReceiveNotification(Notification notification) {
			DeviceNotification deviceNotification = (DeviceNotification) notification;
			onReceiveNotification(deviceNotification.getDeviceGuid(),
					notification);
		}

		private List<String> getDeviceGuids() {
			if (devices != null && !devices.isEmpty()) {
				final List<String> guids = new LinkedList<String>();
				for (DeviceData device : devices) {
					guids.add(device.getId());
				}
				return guids;
			} else {
				return null;
			}
		}
	}

	/**
	 * Send command to the given device.
	 * 
	 * @param device
	 *            Target device.
	 * @param command
	 *            {@link Command} to be sent.
	 */
	public void sendCommand(DeviceData device, final Command command) {
		this.sendCommand(device, command);
	}

	/**
	 * Reload device data. Current device data is updated with instance of
	 * {@link DeviceData} retrieved from the server.
	 * 
	 * @see #onFinishReloadingDeviceData(DeviceData)
	 * @see #onFailReloadingDeviceData()
	 */
	public void reloadDeviceData(DeviceData device) {
		serviceConnection.reloadDeviceData(device);
	}

	/* package */void onReloadDeviceDataFinishedInternal(DeviceData deviceData) {
		final DeviceData device = getDeviceDataWithId(deviceData.getId());
		devices.remove(device);
		devices.add(deviceData);
		onFinishReloadingDeviceData(deviceData);
	}

	private DeviceData getDeviceDataWithId(String deviceId) {
		for (DeviceData device : devices) {
			if (device.getId().equals(deviceId)) {
				return device;
			}
		}
		return null;
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
	protected abstract void onReceiveNotification(String deviceId,
			final Notification notification);
}
