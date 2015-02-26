package com.dataart.android.devicehive.device;

import com.dataart.android.devicehive.DeviceHive;
import com.dataart.android.devicehive.EquipmentData;

import android.util.Log;

/**
 * Represents an abstract equipment which is installed on devices. This class is
 * designed to be subclassed in order to represent specific equipment.
 * Descendants should implement abstract methods of {@link CommandRunner}
 * interface in order to execute commands. Also they may override various
 * callbacks: {@link #onRegisterEquipment()}, {@link #onUnregisterEquipment()},
 * {@link #onStartProcessingCommands()}, {@link #onStopProcessingCommands()},
 * etc.
 */
public abstract class Equipment implements CommandRunner {

	private final EquipmentData equipmentData;
	private Device device;

	/**
	 * Construct equipment with given {@link EquipmentData}.
	 * 
	 * @param equipmentData
	 *            {@link EquipmentData} object which describes equipment
	 *            parameters.
	 */
	public Equipment(EquipmentData equipmentData) {
		this.equipmentData = equipmentData;
	}

	/**
	 * Get equipment data which describes equipment parameters.
	 * 
	 * @return
	 */
	public EquipmentData getEquipmentData() {
		return equipmentData;
	}

	/**
	 * Get device which this equipment attached to.
	 * 
	 * @return Corresponding {@link com.dataart.android.devicehive.device.Device} object or <code>null</code>, if
	 *         equipment isn't attached to any device.
	 */
	public Device getDevice() {
		return device;
	}

	/* package */void setDevice(Device device) {
		this.device = device;
	}

	/**
	 * Send equipment notification. Equipment should be attached to a
	 * {@link com.dataart.android.devicehive.device.Device} and device should be registered in order for notification
	 * to be successfully sent.
	 * 
	 * @param notification
	 *            Notification to be sent.
	 */
	public void sendNotification(EquipmentNotification notification) {
		if (device == null) {
			Log.w(DeviceHive.TAG, "Equipment should be attached to a device in order to be able to send notifications");
		} else {
			device.sendNotification(notification);
		}
	}

	/**
	 * Called as a part of device registration process. Override this method to
	 * perform any additional initialization. This method can be called either
	 * on main thread or some other thread. It depends on
	 * {@link com.dataart.android.devicehive.device.Device#performsEquipmentRegistrationCallbacksAsynchronously()}
	 * method's return value.
	 * 
	 * @return true, if equipment is registered successfully, otherwise returns
	 *         false.
	 */
	protected boolean onRegisterEquipment() {
		return true;
	}

	/**
	 * Called as a part of device deregistration process. Usually happens during
	 * connection loss. Override this method to perform any additional
	 * deinitialization. This method can be called either on main thread or some
	 * other thread. It depends on
	 * {@link com.dataart.android.devicehive.device.Device#performsEquipmentRegistrationCallbacksAsynchronously()}
	 * method's return value.
	 * 
	 * @return true, if equipment is unregistered successfully, otherwise return
	 *         false.
	 */
	protected boolean onUnregisterEquipment() {
		return true;
	}

	/**
	 * Called right after {@link com.dataart.android.devicehive.device.Device#startProcessingCommands()} method is
	 * called. Override this method to perform additional initialization or
	 * other actions.
	 */
	protected void onStartProcessingCommands() {
		// no op
	}

	/**
	 * Called right after {@link com.dataart.android.devicehive.device.Device#stopProcessingCommands()} method is
	 * called. Override this method to perform additional actions when device
	 * stops processing commands.
	 */
	protected void onStopProcessingCommands() {
		// no op
	}

}
