package com.dataart.android.devicehive.client.commands;

import android.os.Parcel;

import com.google.gson.Gson;

/**
 * Abstract base class for commands which poll notifications for given device.
 * 
 */
public abstract class PollNotificationsCommand extends
		NotificationsRetrivalCommand {

	protected final Integer waitTimeout;

	/**
	 * Construct a new command with last received notification timestamp.
	 * 
	 * @param lastNotificationPollTimestamp
	 *            Last received notification timestamp.
	 */
	public PollNotificationsCommand(String lastNotificationPollTimestamp) {
		this(lastNotificationPollTimestamp, null);
	}

	/**
	 * Construct a new command with last received notification timestamp and
	 * wait timeout.
	 * 
	 * @param lastNotificationPollTimestamp
	 *            Last received notification timestamp.
	 * @param waitTimeout
	 *            Waiting timeout in seconds.
	 */
	public PollNotificationsCommand(String lastNotificationPollTimestamp,
			Integer waitTimeout) {
		super(lastNotificationPollTimestamp);
		this.waitTimeout = waitTimeout;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.GET;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeValue(waitTimeout);
	}

	@Override
	protected String toJson(Gson gson) {
		return null;
	}
}
