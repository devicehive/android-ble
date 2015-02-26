package com.dataart.android.devicehive.client.commands;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Poll {@link Notification}s from given devices starting from given timestamp.
 * This request returns immediately if there have been any notifications since
 * given timestamp. In the case when no notifications were found, the method
 * blocks until new notification is received. The blocking period is limited
 * (currently 30 seconds). As a result returns list of {@link Notification}.
 */
public class PollMultipleDeviceNotificationsCommand extends
		PollNotificationsCommand {

	protected final List<String> deviceIds;

	/**
	 * Construct command for given device and last received notification
	 * timestamp.
	 * 
	 * @param deviceIds
	 *            List of device identifiers.
	 * @param lastNotificationPollTimestamp
	 *            Last received notification timestamp.
	 */
	public PollMultipleDeviceNotificationsCommand(List<String> deviceIds,
			String lastNotificationPollTimestamp) {
		this(deviceIds, lastNotificationPollTimestamp, null);
	}
	
	/**
	 * Construct a new command.
	 * 
	 * @param deviceIds
	 *            List of device identifiers.
	 * @param lastNotificationPollTimestamp
	 *            Timestamp which defines starting point in the past for
	 *            notifications.
	 * @param waitTimeout
	 *            Waiting timeout in seconds.
	 */
	public PollMultipleDeviceNotificationsCommand(List<String> deviceIds,
			String lastNotificationPollTimestamp, Integer waitTimeout) {
		super(lastNotificationPollTimestamp, waitTimeout);
		this.deviceIds = deviceIds;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringList(deviceIds);
		super.writeToParcel(dest, flags);
	}

	public static Parcelable.Creator<PollMultipleDeviceNotificationsCommand> CREATOR = new Parcelable.Creator<PollMultipleDeviceNotificationsCommand>() {

		@Override
		public PollMultipleDeviceNotificationsCommand[] newArray(int size) {
			return new PollMultipleDeviceNotificationsCommand[size];
		}

		@Override
		public PollMultipleDeviceNotificationsCommand createFromParcel(
				Parcel source) {
			List<String> deviceIds = new LinkedList<String>();
			source.readStringList(deviceIds);
			return new PollMultipleDeviceNotificationsCommand(deviceIds,
					source.readString(),
					(Integer) source.readValue(CLASS_LOADER));
		}
	};

	@Override
	protected String getRequestPath() {
		String requestPath = "device/notification/poll";
		if (isDeviceGuidsPresent()) {
			requestPath += String.format("?deviceGuids=%s",
					prepareGuidsString(deviceIds));
		}
		if (lastNotificationPollTimestamp != null) {
			requestPath += isDeviceGuidsPresent() ? "&" : "?";
			requestPath += "timestamp="
					+ encodedString(lastNotificationPollTimestamp);
		}
		if (waitTimeout != null) {
			requestPath += (isDeviceGuidsPresent() || lastNotificationPollTimestamp != null) ? "&"
					: "?";
			requestPath += "waitTimeout=" + waitTimeout;
		}
		return requestPath;
	}

	private boolean isDeviceGuidsPresent() {
		return deviceIds != null && !deviceIds.isEmpty();
	}

	private String prepareGuidsString(List<String> guids) {
		if (deviceIds == null || deviceIds.isEmpty()) {
			return null;
		} else {
			final StringBuilder builder = new StringBuilder("");
			String separator = "";
			for (String guid : guids) {
				builder.append(separator).append(guid);
				separator = ",";
			}
			return builder.toString();
		}
	}

	public static class DeviceNotification extends Notification {

		private String deviceGuid;

		public String getDeviceGuid() {
			return deviceGuid;
		}

		/* package */DeviceNotification(int id, String name, String timestamp,
				Serializable parameters) {
			super(id, name, timestamp, parameters);
		}

		/* package */DeviceNotification(String deviceGuid,
				Notification notification) {
			super(notification.getId(), notification.getName(), notification
					.getTimestamp(), notification.getParameters());
			this.deviceGuid = deviceGuid;
		}
	}

	private static class DeviceGuidNotification {
		String deviceGuid;
		Notification notification;
	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {

		Type listType = new TypeToken<ArrayList<DeviceGuidNotification>>() {
		}.getType();

		ArrayList<DeviceGuidNotification> notifications = gson.fromJson(
				response, listType);
		resultData.putParcelableArrayList(NOTIFICATIONS_KEY,
				asDeviceNotificationList(notifications));
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	private static ArrayList<DeviceNotification> asDeviceNotificationList(
			List<DeviceGuidNotification> notifications) {
		ArrayList<DeviceNotification> result = new ArrayList<DeviceNotification>(
				notifications.size());
		for (DeviceGuidNotification guidNotification : notifications) {
			result.add(new DeviceNotification(guidNotification.deviceGuid,
					guidNotification.notification));
		}
		return result;
	}
}
