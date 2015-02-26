package com.dataart.android.devicehive.device.commands;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;

/**
 * Register a new device which parameters are described by given
 * {@link DeviceData} instance.
 * 
 */
public class RegisterDeviceCommand extends DeviceCommand {

	private final static String NAMESPACE = RegisterDeviceCommand.class
			.getName();

	private static final String DEVICE_DATA_KEY = NAMESPACE
			.concat(".DEVICE_DATA_KEY");

	private final DeviceData deviceData;

	/**
	 * Construct a new command with given {@link DeviceData}.
	 * 
	 * @param deviceData
	 *            {@link DeviceData} instance.
	 */
	public RegisterDeviceCommand(DeviceData deviceData) {
		super(deviceData.getId(), deviceData.getKey());
		this.deviceData = deviceData;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.PUT;
	}

	@Override
	protected String getRequestPath() {
		return String.format("device/%s", getEncodedDeviceId());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(deviceData, 0);
	}

	public static Parcelable.Creator<RegisterDeviceCommand> CREATOR = new Parcelable.Creator<RegisterDeviceCommand>() {

		@Override
		public RegisterDeviceCommand[] newArray(int size) {
			return new RegisterDeviceCommand[size];
		}

		@Override
		public RegisterDeviceCommand createFromParcel(Parcel source) {
			return new RegisterDeviceCommand(
					(DeviceData) source.readParcelable(CLASS_LOADER));
		}
	};

	@Override
	protected String toJson(Gson gson) {
		return gson.toJson(deviceData);
	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {
		final DeviceData deviceData = gson.fromJson(response, DeviceData.class);
		resultData.putParcelable(DEVICE_DATA_KEY, deviceData);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	public final static DeviceData getDeviceData(Bundle resultData) {
		return resultData.getParcelable(DEVICE_DATA_KEY);
	}
}
