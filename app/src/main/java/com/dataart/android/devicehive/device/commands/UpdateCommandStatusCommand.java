package com.dataart.android.devicehive.device.commands;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;

/**
 * Update status of given command with given {@link #commandResult}. As a result
 * returns updated {@link Command} instance returned by the server.
 * 
 */
public class UpdateCommandStatusCommand extends DeviceCommand {

	private final static String NAMESPACE = UpdateCommandStatusCommand.class
			.getName();

	private static final String COMMAND_KEY = NAMESPACE.concat(".COMMAND_KEY");

	private final int commandId;
	private final CommandResult commandResult;

	/**
	 * Construct a new update command with given {@link DeviceData} and
	 * identifier of the command to update with {@link CommandResult}.
	 * 
	 * @param deviceId
	 *            Device unique identifier.
	 * @param deviceKey
	 *            Device key.
	 * @param command
	 *            {@link Command} to be updated.
	 * @param commandResult
	 *            {@link CommandResult} object describing command status.
	 */
	public UpdateCommandStatusCommand(String deviceId, String deviceKey, int commandId,
			CommandResult commandResult) {
		super(deviceId, deviceKey);
		this.commandId = commandId;
		this.commandResult = commandResult;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.PUT;
	}

	@Override
	protected String getRequestPath() {
		return String.format("device/%s/command/%d", getEncodedDeviceId(),
				commandId);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeInt(commandId);
		dest.writeParcelable(commandResult, 0);
	}

	public static Parcelable.Creator<UpdateCommandStatusCommand> CREATOR = new Parcelable.Creator<UpdateCommandStatusCommand>() {

		@Override
		public UpdateCommandStatusCommand[] newArray(int size) {
			return new UpdateCommandStatusCommand[size];
		}

		@Override
		public UpdateCommandStatusCommand createFromParcel(Parcel source) {
			return new UpdateCommandStatusCommand(
					source.readString(),
					source.readString(),
					source.readInt(),
					(CommandResult) source.readParcelable(CLASS_LOADER));
		}
	};

	@Override
	protected String toJson(Gson gson) {
		return gson.toJson(commandResult);
	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {
		final Command command = gson.fromJson(response, Command.class);
		resultData.putParcelable(COMMAND_KEY, command);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	public final static Command getUpdatedCommand(Bundle resultData) {
		return resultData.getParcelable(COMMAND_KEY);
	}
}
