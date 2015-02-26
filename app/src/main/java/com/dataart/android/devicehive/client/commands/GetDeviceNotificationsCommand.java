package com.dataart.android.devicehive.client.commands;

import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.Notification;

/**
 * Get all device {@link Notification}s starting from given timestamp. This
 * request returns immediately regardless of whether there have been any
 * notifications since given timestamp or not. As a result returns list of
 * {@link Notification}.
 */
public class GetDeviceNotificationsCommand extends NotificationsRetrivalCommand {

	private final String deviceId;

	/**
	 * Construct a new command.
	 * 
	 * @param deviceId
	 *            Device identifier.
	 * @param lastNotificationPollTimestamp
	 *            Timestamp which defines starting point in the past for
	 *            notifications.
	 */
	public GetDeviceNotificationsCommand(String deviceId,
			String lastNotificationPollTimestamp) {
		super(lastNotificationPollTimestamp);
		this.deviceId = deviceId;
	}

	@Override
	protected String getRequestPath() {
		String requestPath = String.format("device/%s/notification",
				encodedString(deviceId));
		if (lastNotificationPollTimestamp != null) {
			requestPath += "?start="
					+ encodedString(lastNotificationPollTimestamp);
		}
		return requestPath;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(deviceId);
		super.writeToParcel(dest, flags);
	}

	public static Parcelable.Creator<GetDeviceNotificationsCommand> CREATOR = new Parcelable.Creator<GetDeviceNotificationsCommand>() {

		@Override
		public GetDeviceNotificationsCommand[] newArray(int size) {
			return new GetDeviceNotificationsCommand[size];
		}

		@Override
		public GetDeviceNotificationsCommand createFromParcel(Parcel source) {
			return new GetDeviceNotificationsCommand(source.readString(),
					source.readString());
		}
	};
}
