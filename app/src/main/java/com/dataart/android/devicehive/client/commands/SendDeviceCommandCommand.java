package com.dataart.android.devicehive.client.commands;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;

/**
 * Send a new command to the given device. As a result returns {@link Command}
 * instance from the server response.
 * 
 */
public class SendDeviceCommandCommand extends DeviceClientCommand {

	private final static String NAMESPACE = SendDeviceCommandCommand.class
			.getName();

	private static final String COMMAND_KEY = NAMESPACE.concat(".COMMAND_KEY");

	private final String deviceId;
	private final Command command;

	/**
	 * Construct a new command with given device identifier and {@link Command}
	 * instance.
	 * 
	 * @param deviceId
	 *            Device unique identifier.
	 * @param command
	 *            {@link Command} instance.
	 * @param notification
	 *            {@link Notification} to be sent on behalf of given device.
	 */
	public SendDeviceCommandCommand(String deviceId, Command command) {
		this.deviceId = deviceId;
		this.command = command;
	}

	/**
	 * Get {@link Command} to be sent.
	 * 
	 * @return {@link Command} instance.
	 */
	public Command getCommand() {
		return command;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.POST;
	}

	@Override
	protected String getRequestPath() {
		return String.format("device/%s/command", encodedString(deviceId));
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(deviceId);
		dest.writeParcelable(command, 0);
	}

	public static Parcelable.Creator<SendDeviceCommandCommand> CREATOR = new Parcelable.Creator<SendDeviceCommandCommand>() {

		@Override
		public SendDeviceCommandCommand[] newArray(int size) {
			return new SendDeviceCommandCommand[size];
		}

		@Override
		public SendDeviceCommandCommand createFromParcel(Parcel source) {
			return new SendDeviceCommandCommand(source.readString(),
					(Command) source.readParcelable(CLASS_LOADER));
		}
	};

	@Override
	protected String toJson(Gson gson) {
		return gson.toJson(command);
	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {
		final Command command = gson.fromJson(response, Command.class);
		resultData.putParcelable(COMMAND_KEY, command);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	/**
	 * Get sent {@link Command} instance returned by the server.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return {@link Command} instance returned by the server.
	 */
	public final static Command getSentCommand(Bundle resultData) {
		return resultData.getParcelable(COMMAND_KEY);
	}
}
