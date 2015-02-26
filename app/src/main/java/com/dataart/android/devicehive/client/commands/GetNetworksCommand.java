package com.dataart.android.devicehive.client.commands;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.Network;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Get a list of {@link Network} available for current user.
 * 
 */
public class GetNetworksCommand extends DeviceClientCommand {

	private final static String NAMESPACE = GetNetworksCommand.class.getName();

	private static final String NETWORKS_KEY = NAMESPACE
			.concat(".NETWORKS_KEY");

	public GetNetworksCommand() {
		super();
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.GET;
	}

	@Override
	protected String getRequestPath() {
		return "network";
	}

	public static Parcelable.Creator<GetNetworksCommand> CREATOR = new Parcelable.Creator<GetNetworksCommand>() {

		@Override
		public GetNetworksCommand[] newArray(int size) {
			return new GetNetworksCommand[size];
		}

		@Override
		public GetNetworksCommand createFromParcel(Parcel source) {
			return new GetNetworksCommand();
		}
	};

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {
		Type listType = new TypeToken<ArrayList<Network>>() {
		}.getType();
		final ArrayList<Network> networks = gson.fromJson(response,
				listType);
		resultData.putParcelableArrayList(NETWORKS_KEY, networks);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	/**
	 * Get a list of {@link Network} available for current user.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return A list of {@link Network} available for current user.
	 */
	public final static List<Network> getNetworks(Bundle resultData) {
		return resultData.getParcelableArrayList(NETWORKS_KEY);
	}

	@Override
	protected String toJson(Gson gson) {
		return null;
	}
}
