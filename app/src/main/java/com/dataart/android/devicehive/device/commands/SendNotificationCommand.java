package com.dataart.android.devicehive.device.commands;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;

/**
 * Send a new notification from the given device. As a result returns
 * {@link Notification} instance from the server response.
 * 
 */
public class SendNotificationCommand extends DeviceCommand {

	private final static String NAMESPACE = SendNotificationCommand.class
			.getName();

	private static final String NOTIFICATION_DATA_KEY = NAMESPACE
			.concat(".NOTIFICATION_DATA_KEY");

	private final Notification notification;

	/**
	 * Construct a new command with given {@link DeviceData} and last received
	 * command timestamp.
	 * 
	 * @param deviceId
	 *            Device unique identifier.
	 * @param deviceKey
	 *            Device key.
	 * @param notification
	 *            {@link Notification} to be sent on behalf of given device.
	 */
	public SendNotificationCommand(String deviceId, String deviceKey,
			Notification notification) {
		super(deviceId, deviceKey);
		this.notification = notification;
	}

	/**
	 * Get {@link Notification} to be sent.
	 * @return {@link Notification} instance.
	 */
	public Notification getNotification() {
		return notification;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.POST;
	}

	@Override
	protected String getRequestPath() {
		return String.format("device/%s/notification", getEncodedDeviceId());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeParcelable(notification, 0);
	}

	public static Parcelable.Creator<SendNotificationCommand> CREATOR = new Parcelable.Creator<SendNotificationCommand>() {

		@Override
		public SendNotificationCommand[] newArray(int size) {
			return new SendNotificationCommand[size];
		}

		@Override
		public SendNotificationCommand createFromParcel(Parcel source) {
			return new SendNotificationCommand(
					source.readString(),
					source.readString(),
					(Notification) source.readParcelable(CLASS_LOADER));
		}
	};

	@Override
	protected String toJson(Gson gson) {
		return gson.toJson(notification);
	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {
		final Notification notification = gson.fromJson(response,
				Notification.class);
		resultData.putParcelable(NOTIFICATION_DATA_KEY, notification);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	public final static Notification getNotification(Bundle resultData) {
		return resultData.getParcelable(NOTIFICATION_DATA_KEY);
	}
}
