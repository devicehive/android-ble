package com.dataart.android.devicehive.device;

import android.content.Context;

import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Notification;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a device, a unit that executes commands and communicates to the
 * server. This class is abstract and designed to be subclassed in order to
 * represent specific device. Descendants should implement abstract methods of
 * {@link CommandRunner} interface in order to execute commands. Also they may
 * override various callbacks: {@link #onStartRegistration()},
 * {@link #onFinishRegistration()}, {@link #onStartProcessingCommands()},
 * {@link #onStopProcessingCommands()},
 * {@link #onStartSendingNotification(Notification)},
 * {@link #onFinishSendingNotification(Notification)}, etc.
 * 
 */
public abstract class Device implements CommandRunner {

	private DeviceData deviceData;
	private final DeviceServiceConnection serviceConnection;

	protected boolean isRegistered = false;

	private final List<Equipment> equipmentList = new LinkedList<Equipment>();

	/**
	 * Construct device with given {@link android.content.Context} and {@link DeviceData}
	 * objects.
	 * 
	 * @param context
	 *            {@link android.content.Context} object. In most cases this should be
	 *            application context which stays alive during the entire life
	 *            of an application.
	 * @param deviceData
	 *            {@link DeviceData} object which describes various device
	 *            parameters.
	 */
	public Device(Context context, DeviceData deviceData) {
		this.serviceConnection = new DeviceServiceConnection(context);
		serviceConnection.setDevice(this);
		this.deviceData = deviceData;
	}

	/**
	 * Get {@link DeviceData} object which describes various device parameters.
	 * 
	 * @return {@link DeviceData} object.
	 */
	public DeviceData getDeviceData() {
		return deviceData;
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
	 * Enable or disable debug log messages.
	 * 
	 * @param enabled
	 *            Whether debug log messages enabled or not.
	 */
	public void setDebugLoggingEnabled(boolean enabled) {
		this.serviceConnection.setDebugLoggingEnabled(enabled);
	}

	/**
	 * Set timestamp of the last received command. This value is used to reduce
	 * amount of commands received from the server as a result of poll request
	 * to only those of them which were received by the server later than the
	 * time defined by given timestamp.
	 * 
	 * @param timestamp
	 *            Timestamp of the last received command.
	 */
	public void setLastCommandPollTimestamp(String timestamp) {
		this.serviceConnection.setLastCommandPollTimestamp(timestamp);
	}

	/**
	 * Set command poll waiting timeout in seconds (default: 30 seconds,
	 * maximum: 60 seconds). Specify 0 to disable waiting.
	 * 
	 * @param timeout
	 *            Command poll waiting timeout in seconds.
	 */
	public void setCommandPollWaitTimeout(Integer timeout) {
		this.serviceConnection.setCommandPollWaitTimeout(timeout);
	}

	/**
	 * Initiate device registration. You should set Device Hive service URL
	 * before performing device registration.
	 * 
	 * @see {@link #setApiEnpointUrl(String)}, {@link #onStartRegistration()},
	 *      {@link #onFinishRegistration()}, {@link #onFailRegistration()}.
	 */
	public void registerDevice() {
		if (getApiEndpointUrl() == null) {
			throw new IllegalStateException(
					"API Endpoint URL should be set before registering device");
		}
		isRegistered = false;
		onStartRegistration();
		serviceConnection.registerDevice();
	}

	/**
	 * Unregister device. Also unregisters all attached equipment.
	 */
	public void unregisterDevice() {
		if (isProcessingCommands()) {
			stopProcessingCommands();
		}
		isRegistered = false;
		serviceConnection.unregisterDevice();
	}

	/**
	 * Check if this device is registered.
	 * 
	 * @return true, if this device is registered, otherwise returns false.
	 */
	public boolean isRegistered() {
		return isRegistered;
	}

	/**
	 * Check if this device is processing command, i.e. performs commands
	 * polling and execution.
	 * 
	 * @return true, if this device is performing commands polling and
	 *         execution, otherwise returns false.
	 */
	public boolean isProcessingCommands() {
		return serviceConnection.isProcessingCommands();
	}

	/**
	 * Send device notification. Device should registered to be able to send
	 * notifications.
	 * 
	 * @param notification
	 *            {@link Notification} to be sent.
	 * @see {@link #registerDevice()}.
	 */
	public void sendNotification(Notification notification) {
//		if (!isRegistered) {
//			Log.w(DeviceHive.TAG,
//					"Device should be registered before sending notifications");
//		} else
        {
			serviceConnection.sendNotification(notification);
		}
	}

	/**
	 * Start processing commands. Device will start performing commands polling
	 * and execution. Device should be registered before processing commands.
	 * 
	 * @see {@link #registerDevice()}.
	 */
	public void startProcessingCommands() {
//		if (!isRegistered || false) {
//			Log.w(DeviceHive.TAG,
//					"Device should be registered before starting receiving commands");
//		}  else
        {
			onStartProcessingCommandsInternal();
			serviceConnection.startProcessingCommands();
		}
	}

	/**
	 * Stop processing commands.
	 */
	public void stopProcessingCommands() {
		serviceConnection.stopProcessingCommands();
		onStopProcessingCommandsInternal();
	}

	/**
	 * Reload device data. Current device data is updated with instance of
	 * {@link DeviceData} retrieved from the server.
	 * 
	 * @see #onFinishReloadingDeviceData(DeviceData)
	 * @see #onFailReloadingDeviceData()
	 */
	public void reloadDeviceData() {
		serviceConnection.reloadDeviceData();
	}

	/**
	 * Attach given {@link Equipment} to the device. {@link Equipment} should be
	 * attached to the device in order to be able to process commands, send
	 * notifications and receive various callback method calls. Equipment
	 * <b>MUST NOT</b> be attached to any other device. Device <b>MUST NOT</b>
	 * be registered. Changing device equipment after registration isn't
	 * supported.
	 * 
	 * @param equipment
	 *            {@link Equipment} to attach.
	 */
	public void attachEquipment(Equipment equipment) {
		if (equipment.getDevice() != null) {
			throw new IllegalArgumentException(
					"Equiment has already been attached to other device");
		} else if (isRegistered) {
			throw new IllegalStateException(
					"Device has already been registered. You cannot attach equipment after a device is registered");
		} else {
			equipment.setDevice(this);
			this.equipmentList.add(equipment);
			this.deviceData.addEquipment(equipment.getEquipmentData());
		}
	}

	/**
	 * Get equipment list of the device.
	 * 
	 * @return All previously attached {@link Equipment} objects.
	 */
	public List<Equipment> getEquipment() {
		return equipmentList;
	}

	/**
	 * Whether this device should perform attached {@link Equipment}'s
	 * registration/unregistration callbacks on some other thread.
	 * 
	 * @return true, if attached {@link Equipment}'s registration/unregistration
	 *         callbacks should be performed on some other thread, otherwise
	 *         return true.
	 * @see {@link Equipment#onRegisterEquipment()},
	 *      {@link Equipment#onUnregisterEquipment()}.
	 */
	protected boolean performsEquipmentRegistrationCallbacksAsynchronously() {
		return false;
	}

	/**
	 * Called at the very beginning of device registration process before
	 * sending network requests to the server. Override this method if you need
	 * to perform any additional actions right before registration.
	 */
	protected void onStartRegistration() {
		// no op
	}

	/**
	 * Called right after device registration process is finished. Override this
	 * method if you need to perform any additional actions right after
	 * registration.
	 */
	protected void onFinishRegistration() {
		// no op
	}

	/**
	 * Called if device fails to register. Override this method if you need to
	 * perform additional actions such as restarting registration.
	 */
	protected void onFailRegistration() {
		// no op
	}

	/**
	 * Called right after {@link #startProcessingCommands()} method is called.
	 * Override this method to perform additional actions before the device
	 * starts processing commands.
	 */
	protected void onStartProcessingCommands() {
		// no op
	}

	/**
	 * Called right after {@link #stopProcessingCommands()} method is called.
	 * Override this method to perform additional actions right after the device
	 * stops processing commands.
	 */
	protected void onStopProcessingCommands() {
		// no op
	}

	/**
	 * Called when device finishes reloading device data from the server.
	 * 
	 * @param deviceData
	 *            {@link DeviceData} instance returned by the server.
	 */
	protected void onFinishReloadingDeviceData(DeviceData deviceData) {
		// no op
	}

	/**
	 * Called when device to reload device data from the server.
	 */
	protected void onFailReloadingDeviceData() {
		// no op
	}

	/**
	 * Called when device or equipment notification is about to be sent.
	 * Override this method to perform additional actions before a notification
	 * is sent.
	 * 
	 * @param notification
	 *            {@link Notification} object.
	 */
	protected void onStartSendingNotification(Notification notification) {
		// no op
	}

	/**
	 * Called when device or equipment notification has been sent. Override this
	 * method to perform additional actions after a notification is sent.
	 * 
	 * @param notification
	 *            {@link Notification} object.
	 */
	protected void onFinishSendingNotification(Notification notification) {
		// no op
	}

	/**
	 * Called when device failed to send notification. Override this method to
	 * perform any additional initialization or customization.
	 * 
	 * @param notification
	 *            {@link Notification} object.
	 */
	protected void onFailSendingNotification(Notification notification) {
		// no op
	}

	/**
	 * Get {@link android.content.Context} which was used to create {@link Device} object.
	 * 
	 * @return {@link android.content.Context} which was used to create {@link Device} object.
	 */
	protected Context getContext() {
		return serviceConnection.getContext();
	}

	/**
	 * Run given {@link Runnable} on the main thread. Helper method.
	 * 
	 * @param runnable
	 *            {@link Runnable} to execute on the main thread.
	 */
	protected void runOnMainThread(Runnable runnable) {
		serviceConnection.runOnMainThread(runnable);
	}

	/* package */Equipment getEquipmentWithCode(String equipmentCode) {
		if (equipmentCode != null) {
			for (Equipment eq : equipmentList) {
				if (equipmentCode.equals(eq.getEquipmentData().getCode())) {
					return eq;
				}
			}
		}
		return null;
	}

	/* package */void equipmentRegistrationFinished(boolean result) {
		isRegistered = result;
		if (result) {
			onFinishRegistration();
		} else {
			onFailRegistration();
		}
	}

	/* package */void equipmentUnregistrationFinished(boolean result) {
		// no op
	}

	/* package */void onReloadDeviceDataFinishedInternal(DeviceData deviceData) {
		updateDeviceData(deviceData);
		onFinishReloadingDeviceData(deviceData);
	}

	private void updateDeviceData(DeviceData newDeviceData) {
		this.deviceData = new DeviceData(deviceData.getId(),
				deviceData.getKey(), newDeviceData.getName(),
				newDeviceData.getStatus(),
				newDeviceData.getDeviceClass());
		this.deviceData.setData((Serializable) newDeviceData.getData());
		for (Equipment equipment : equipmentList) {
			this.deviceData.addEquipment(equipment.getEquipmentData());
		}
	}

	/* package */void onReloadDeviceDataFailedInternal() {
		onFailReloadingDeviceData();
	}

	private void onStartProcessingCommandsInternal() {
		onStartProcessingCommands();
		for (Equipment equipment : equipmentList) {
			equipment.onStartProcessingCommands();
		}
	}

	private void onStopProcessingCommandsInternal() {
		for (Equipment equipment : equipmentList) {
			equipment.onStopProcessingCommands();
		}
		onStopProcessingCommands();
	}
}
