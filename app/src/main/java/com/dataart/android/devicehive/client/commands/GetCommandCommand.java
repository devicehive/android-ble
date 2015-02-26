package com.dataart.android.devicehive.client.commands;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;

/**
 * Get device command with given identifier. As a result returns {@link Command}
 * instance.
 */
public class GetCommandCommand extends DeviceClientCommand {

	private final static String NAMESPACE = GetCommandCommand.class.getName();

	private static final String COMMAND_KEY = NAMESPACE.concat(".COMMAND_KEY");

	private final String deviceId;
	private final int commandId;

	/**
	 * Construct a new command.
	 * 
	 * @param deviceId
	 *            Device unique identifier.
	 * @param commandId
	 *            Command identifier.
	 */
	public GetCommandCommand(String deviceId, int commandId) {
		this.deviceId = deviceId;
		this.commandId = commandId;
	}

	@Override
	protected String toJson(Gson gson) {
		return null;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.GET;
	}

	@Override
	protected String getRequestPath() {
		String requestPath = String.format("device/%s/command/%d",
				encodedString(deviceId), commandId);
		return requestPath;
	}

	public static Parcelable.Creator<GetCommandCommand> CREATOR = new Parcelable.Creator<GetCommandCommand>() {

		@Override
		public GetCommandCommand[] newArray(int size) {
			return new GetCommandCommand[size];
		}

		@Override
		public GetCommandCommand createFromParcel(Parcel source) {
			return new GetCommandCommand(source.readString(), source.readInt());
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(deviceId);
		dest.writeInt(commandId);
	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {

		final Command command = gson.fromJson(response, Command.class);
		resultData.putParcelable(COMMAND_KEY, command);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	/**
	 * Get {@link Command} object from response {@link android.os.Bundle} container.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return {@link Command} instance.
	 */
	public final static Command getDeviceCommand(Bundle resultData) {
		return resultData.getParcelable(COMMAND_KEY);
	}

}
