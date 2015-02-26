package com.dataart.android.devicehive.client.commands;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Get all existing devices. As a result returns a list of {@link DeviceData}.
 * Can be executed only by users with administrative privileges.
 * 
 */
public class GetDevicesCommand extends DeviceClientCommand {

	private final static String NAMESPACE = GetDevicesCommand.class.getName();

	private static final String DEVICES_KEY = NAMESPACE.concat(".DEVICES_KEY");

	/**
	 * Create a new command.
	 */
	public GetDevicesCommand() {
		super();
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.GET;
	}

	@Override
	protected String getRequestPath() {
		return "device";
	}

	public static Parcelable.Creator<GetDevicesCommand> CREATOR = new Parcelable.Creator<GetDevicesCommand>() {

		@Override
		public GetDevicesCommand[] newArray(int size) {
			return new GetDevicesCommand[size];
		}

		@Override
		public GetDevicesCommand createFromParcel(Parcel source) {
			return new GetDevicesCommand();
		}
	};

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {

		Type listType = new TypeToken<ArrayList<DeviceData>>() {
		}.getType();
		final ArrayList<DeviceData> devices = gson.fromJson(response,
				listType);
		resultData.putParcelableArrayList(DEVICES_KEY, devices);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	/**
	 * Get a list of all existing devices from response {@link android.os.Bundle}
	 * container.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return A list of {@link DeviceData}.
	 */
	public final static List<DeviceData> getDevices(Bundle resultData) {
		return resultData.getParcelableArrayList(DEVICES_KEY);
	}

	@Override
	protected String toJson(Gson gson) {
		return null;
	}
}
