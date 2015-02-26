package com.dataart.android.devicehive.device;

import java.util.HashMap;

import com.dataart.android.devicehive.Notification;

/**
 * Represents a {@link Notification} which is usually sent by an
 * {@link com.dataart.android.devicehive.device.Equipment} to update its state.
 */
public class EquipmentNotification extends Notification {

	/**
	 * Construct a notification with given equipment code and additional
	 * equipment parameters dictionary.
	 * 
	 * @param equipmentCode
	 *            Equipment code.
	 * @param parameters
	 *            Equipment parameters dictionary.
	 */
	public EquipmentNotification(String equipmentCode,
			HashMap<String, Object> parameters) {
		super("equipment", equipmentParameters(equipmentCode, parameters));
	}

	private static HashMap<String, Object> equipmentParameters(
			String equipmentCode, HashMap<String, Object> parameters) {
		final HashMap<String, Object> equipmentParameters = new HashMap<String, Object>(
				parameters);
		parameters.put("equipment", equipmentCode);
		return equipmentParameters;
	}
}
