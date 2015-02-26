package com.dataart.android.devicehive.device.commands;

import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.DeviceData;

/**
 * Get commands for given device starting from given date timestamp. Server
 * returns response immediately regardless of whether there are any commands for
 * given device.
 */
public class GetDeviceCommandsCommand extends DeviceCommandsRetrivalCommand {

	/**
	 * Construct a new command with given {@link DeviceData} and last received
	 * command timestamp.
	 * 
	 * @param deviceId
	 *            Device unique identifier.
	 * @param deviceKey
	 *            Device key.
	 * @param lastCommandPollTimestamp
	 *            Timestamp of the last received command. If null value is
	 *            passed then server will return all command it's received so
	 *            far.
	 */
	public GetDeviceCommandsCommand(String deviceId, String deviceKey,
			String lastCommandPollTimestamp) {
		super(deviceId, deviceKey, lastCommandPollTimestamp);
	}

	@Override
	protected String getRequestPath() {
		String requestPath = String.format("device/%s/command",
				getEncodedDeviceId());
		if (lastCommandPollTimestamp != null) {
			requestPath += "?start=" + encodedString(lastCommandPollTimestamp);
		}
		return requestPath;
	}

	public static Parcelable.Creator<GetDeviceCommandsCommand> CREATOR = new Parcelable.Creator<GetDeviceCommandsCommand>() {

		@Override
		public GetDeviceCommandsCommand[] newArray(int size) {
			return new GetDeviceCommandsCommand[size];
		}

		@Override
		public GetDeviceCommandsCommand createFromParcel(Parcel source) {
			return new GetDeviceCommandsCommand(source.readString(),
					source.readString(), source.readString());
		}
	};

}
