package com.dataart.android.devicehive.client.commands;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Network;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;

/**
 * Get all devices which belong to given network. As a result returns a list of
 * {@link DeviceData} objects.
 * 
 */
public class GetNetworkDevicesCommand extends DeviceClientCommand {

	private final static String NAMESPACE = GetNetworkDevicesCommand.class
			.getName();

	private static final String NETWORK_KEY = NAMESPACE.concat(".NETWORK_KEY");
	private static final String DEVICES_KEY = NAMESPACE.concat(".DEVICES_KEY");

	private final int networkId;

	/**
	 * Construct a new command with given {@link Network} identifier.
	 * 
	 * @param networkId
	 *            {@link Network} identifier.
	 */
	public GetNetworkDevicesCommand(int networkId) {
		this.networkId = networkId;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.GET;
	}

	@Override
	protected String getRequestPath() {
		return String.format("network/%d", networkId);
	}

	@Override
	protected String toJson(Gson gson) {
		return null;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(networkId);
	}

	public static Parcelable.Creator<GetNetworkDevicesCommand> CREATOR = new Parcelable.Creator<GetNetworkDevicesCommand>() {

		@Override
		public GetNetworkDevicesCommand[] newArray(int size) {
			return new GetNetworkDevicesCommand[size];
		}

		@Override
		public GetNetworkDevicesCommand createFromParcel(Parcel source) {
			return new GetNetworkDevicesCommand(source.readInt());
		}
	};

	private class NetworkExtended extends Network {

		ArrayList<DeviceData> devices;

		NetworkExtended(String key, String name, String description) {
			super(key, name, description);
		}

	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {

		final NetworkExtended networkExtended = gson.fromJson(response,
				NetworkExtended.class);
		resultData.putParcelable(NETWORK_KEY, networkExtended);
		resultData.putParcelableArrayList(DEVICES_KEY, networkExtended.devices);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	/**
	 * Get target {@link Network} object.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return {@link Network} instance.
	 */
	public final static Network getNetwork(Bundle resultData) {
		return resultData.getParcelable(NETWORK_KEY);
	}

	/**
	 * Get a list of {@link DeviceData} which belong to target {@link Network}
	 * object.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return A list of {@link DeviceData} which belong to target
	 *         {@link Network} object.
	 */
	public final static List<DeviceData> getNetworkDevices(Bundle resultData) {
		return resultData.getParcelableArrayList(DEVICES_KEY);
	}

}
