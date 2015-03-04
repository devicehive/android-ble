package com.dataart.android.devicehive.client.commands;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.DeviceClass;
import com.dataart.android.devicehive.EquipmentData;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;

/**
 * Command which retrieves equipment for {@link DeviceClass}. As a result
 * returns list of {@link EquipmentData}.
 */
public class GetDeviceClassEquipmentCommand extends DeviceClientCommand {

	private final static String NAMESPACE = GetDeviceClassEquipmentCommand.class
			.getName();

	private static final String DEVICE_CLASS_KEY = NAMESPACE
			.concat(".DEVICE_CLASS_KEY");
	private static final String EQUIPMENT_KEY = NAMESPACE
			.concat(".EQUIPMENT_KEY");

	private final int deviceClassId;

	/**
	 * Construct a new command with given {@link DeviceClass}.
	 * 
	 * @param deviceClassId
	 *            {@link DeviceClass} identifier.
	 */
	public GetDeviceClassEquipmentCommand(int deviceClassId) {
		this.deviceClassId = deviceClassId;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.GET;
	}

	@Override
	protected String getRequestPath() {
		return String.format("device/class/%d", deviceClassId);
	}

	@Override
	protected String toJson(Gson gson) {
		return null;
	}

	private class DeviceClassEquipment extends DeviceClass {

		ArrayList<EquipmentData> equipment;

		DeviceClassEquipment(String name, String version,
				boolean isPermanent, int offlineTimeout) {
			super(name, version, isPermanent, offlineTimeout);
		}

	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(deviceClassId);
	}

	public static Parcelable.Creator<GetDeviceClassEquipmentCommand> CREATOR = new Parcelable.Creator<GetDeviceClassEquipmentCommand>() {

		@Override
		public GetDeviceClassEquipmentCommand[] newArray(int size) {
			return new GetDeviceClassEquipmentCommand[size];
		}

		@Override
		public GetDeviceClassEquipmentCommand createFromParcel(Parcel source) {
			return new GetDeviceClassEquipmentCommand(
					source.readInt());
		}
	};

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {

		final DeviceClassEquipment deviceClassEquipment = gson.fromJson(
				response, DeviceClassEquipment.class);
		resultData.putParcelable(DEVICE_CLASS_KEY, deviceClassEquipment);
		resultData.putParcelableArrayList(EQUIPMENT_KEY,
				deviceClassEquipment.equipment);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	public final static DeviceClass getDeviceClass(Bundle resultData) {
		return resultData.getParcelable(DEVICE_CLASS_KEY);
	}

	/**
	 * Get a list of {@link EquipmentData} which belong to target
	 * {@link DeviceClass} object.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return A list of {@link EquipmentData} which belong to target
	 *         {@link DeviceClass} object.
	 */
	public final static List<EquipmentData> getEquipment(Bundle resultData) {
		return resultData.getParcelableArrayList(EQUIPMENT_KEY);
	}

}
