package com.dataart.android.devicehive.device;

import java.util.HashMap;

import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Notification;

/**
 * Represents a device status notification which is sent by {@link com.dataart.android.devicehive.device.Device} when
 * its status changes.
 */
public class DeviceStatusNotification extends Notification {

	/**
	 * Predefined "Online" device status notification.
	 */
	public static final DeviceStatusNotification STATUS_ONLINE = new DeviceStatusNotification(
			DeviceData.DEVICE_STATUS_ONLINE);

	/**
	 * Predefined "OK" device status notification.
	 */
	public static final DeviceStatusNotification STATUS_OK = new DeviceStatusNotification(
			DeviceData.DEVICE_STATUS_OK);

	/**
	 * Construct device status notification with given device status.
	 * 
	 * @param deviceStatus
	 *            Device status value.
	 */
	public DeviceStatusNotification(String deviceStatus) {
		super("DeviceStatus", parametersWithDeviceStatus(deviceStatus));
	}

	private static HashMap<String, Object> parametersWithDeviceStatus(
			String deviceStatus) {
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("status", deviceStatus);
		return parameters;
	}
}
