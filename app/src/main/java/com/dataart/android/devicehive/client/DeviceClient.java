package com.dataart.android.devicehive.client;

import android.content.Context;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Notification;

/**
 * An abstract device client which provides basic API for clients. This class is
 * abstract and designed to be subclassed.
 */
/* package */abstract class DeviceClient {

	protected final Context context;
	protected ClientServiceConnection serviceConnection;

	/**
	 * Construct client with given {@link android.content.Context}.
	 * 
	 * @param context
	 *            {@link android.content.Context} object. In most cases this should be
	 *            application context which stays alive during the entire life
	 *            of an application.
	 */
	protected DeviceClient(Context context) {
		this.context = context;
	}

	/**
	 * Set Device Hive service URL. This method <b>MUST</b> be called before
	 * performing registration and other subsequent network communications.
	 * 
	 * @param url
	 *            URL of Device Hive service.
	 */
	public void setApiEnpointUrl(String url) {
		serviceConnection.setApiEndpointUrl(url);
	}

	/**
	 * Get previously set Device Hive service URL.
	 * 
	 * @return URL of Device Hive service.
	 */
	public String getApiEndpointUrl() {
		return serviceConnection.getApiEndpointUrl();
	}

	/**
	 * Set Basic Authorisation credentials.
	 * 
	 * @param accessKey
	 *            accessKey
	 */
	public void setAuthorisation(String accessKey) {
		serviceConnection.setAuthorisation(accessKey);
	}

	/**
	 * Enable or disable debug log messages.
	 * 
	 * @param enabled
	 *            Whether debug log messages enabled or not.
	 */
	public void setDebugLoggingEnabled(boolean enabled) {
		serviceConnection.setDebugLoggingEnabled(enabled);
	}

	/**
	 * Set timestamp of the last received notification. This value is used to
	 * reduce amount of notifications received from the server as a result of
	 * poll request to only those of them which were received by the server
	 * later than the time defined by given timestamp. If not specified, the
	 * server's timestamp is taken instead.
	 * 
	 * @param timestamp
	 *            Timestamp of the last received notification.
	 */
	public void setLastNotificationPollTimestamp(String timestamp) {
		serviceConnection.setLastNotificationPollTimestamp(timestamp);
	}

	/**
	 * Get the timestamp of the last received notification.
	 * @return Timestamp of the last received notification.
	 */
	public String getLastNotificationPollTimestamp() {
		return serviceConnection.getLastNotificationPollTimestamp();
	}
	
	/**
	 * Set notification poll waiting timeout in seconds (default: 30 seconds,
	 * maximum: 60 seconds). Specify 0 to disable waiting.
	 * 
	 * @param timeout
	 *            Notification poll waiting timeout in seconds.
	 */
	public void setNotificationPollWaitTimeout(Integer timeout) {
		this.serviceConnection.setNotificationPollWaitTimeout(timeout);
	}

	/**
	 * Check if this client is receiving notifications, i.e. performs
	 * notification polling.
	 * 
	 * @return true, if this client is performing notification polling,
	 *         otherwise returns false.
	 */
	public boolean isReceivingNotifications() {
		return serviceConnection.isReceivingNotifications();
	}

	/**
	 * Start receiving notifications. Client will start polling server for new
	 * notifications.
	 */
	public void startReceivingNotifications() {
		onStartReceivingNotifications();
		serviceConnection.startReceivingNotifications();
	}

	/**
	 * Stop receiving notifications.
	 */
	public void stopReceivingNotifications() {
		serviceConnection.stopReceivingNotifications();
		onStopReceivingNotifications();
	}

	/**
	 * Get context which was used to create this client.
	 * 
	 * @return {@link android.content.Context} was used to create this client.
	 */
	public Context getContext() {
		return serviceConnection.getContext();
	}

	/**
	 * Run given runnable on main thread. Helper method.
	 * 
	 * @param runnable
	 *            Instance which implements {@link Runnable} interface.
	 */
	protected void runOnMainThread(Runnable runnable) {
		serviceConnection.runOnMainThread(runnable);
	}

	/**
	 * Called right after {@link #startReceivingNotifications()} method is
	 * called. Override this method to perform additional actions before the
	 * client starts receiving notifications.
	 */
	protected void onStartReceivingNotifications() {
		// no op
	}

	/**
	 * Called right after {@link #stopReceivingNotifications()} method is
	 * called. Override this method to perform additional actions right after
	 * the device stops receiving notifications.
	 */
	protected void onStopReceivingNotifications() {
		// no op
	}

	/**
	 * Called when {@link Command} is about to be sent. Override this method to
	 * perform additional actions before a command is sent.
	 * 
	 * @param command
	 *            {@link Command} object.
	 */
	protected void onStartSendingCommand(Command command) {
		// no op
	}

	/**
	 * Called when {@link Command} has been sent to the device. Override this
	 * method to perform additional actions after a command is sent to the
	 * device.
	 * 
	 * @param command
	 *            {@link Command} object.
	 */
	protected void onFinishSendingCommand(Command command) {
		// no op
	}

	/**
	 * Called when client failed to send command to the device. Override this
	 * method to perform any extra actions.
	 * 
	 * @param command
	 *            {@link Command} object.
	 */
	protected void onFailSendingCommand(Command command) {
		// no op
	}

	/**
	 * Called when device client finishes reloading device data from the server.
	 * 
	 * @param deviceData
	 *            {@link DeviceData} instance returned by the server.
	 */
	protected void onFinishReloadingDeviceData(DeviceData deviceData) {
		// no op
	}

	/**
	 * Called when device client fails to reload device data from the server.
	 */
	protected void onFailReloadingDeviceData() {
		// no op
	}

	/**
	 * Check whether given notification should be handled asynchronously. If so
	 * {@link #onReceiveNotification(Notification)} method is called on some
	 * other, not UI thread.
	 * 
	 * @param notification
	 *            {@link Notification} instance.
	 * @return true, if given notification should be handled asynchronously,
	 *         otherwise return false.
	 */
	protected abstract boolean shouldReceiveNotificationAsynchronously(
			final Notification notification);

	/* package */void onReloadDeviceDataFinishedInternal(DeviceData deviceData) {
		onFinishReloadingDeviceData(deviceData);
	}

	/* package */void onReloadDeviceDataFailedInternal() {
		onFailReloadingDeviceData();
	}

	/* package */void setServiceConnection(
			ClientServiceConnection serviceConnection) {
		this.serviceConnection = serviceConnection;
		this.serviceConnection.setClient(this);
	}
}
