package com.dataart.android.devicehive.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.dataart.android.devicehive.ApiInfo;
import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.DeviceHive;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.client.commands.GetDeviceCommand;
import com.dataart.android.devicehive.client.commands.NotificationsRetrivalCommand;
import com.dataart.android.devicehive.client.commands.PollDeviceNotificationsCommand;
import com.dataart.android.devicehive.client.commands.PollMultipleDeviceNotificationsCommand;
import com.dataart.android.devicehive.client.commands.SendDeviceCommandCommand;
import com.dataart.android.devicehive.commands.GetApiInfoCommand;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.dataart.android.devicehive.network.NetworkCommand;
import com.dataart.android.devicehive.network.NetworkCommandConfig;
import com.dataart.android.devicehive.network.ServiceConnection;

/* package */abstract class ClientServiceConnection extends ServiceConnection {

	private DeviceClient client;

	private final Queue<Notification> notificationQueue = new LinkedList<Notification>();

	private boolean isReceivingNotifications = false;

	private String username;
	private String password;

	private boolean isPollRequestInProgress = false;

	private String lastNotificationPollTimestamp;
	private Integer notificationPollWaitTimeout;

	public ClientServiceConnection(Context context) {
		super(context);
	}

	/* package */void setAuthorisation(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/* package */void setLastNotificationPollTimestamp(String timestamp) {
		this.lastNotificationPollTimestamp = timestamp;
	}

	/* package */String getLastNotificationPollTimestamp() {
		return lastNotificationPollTimestamp;
	}

	/* package */void setNotificationPollWaitTimeout(Integer timeout) {
		this.notificationPollWaitTimeout = timeout;
	}

	/* package */void sendCommand(DeviceData deviceData, Command command) {
		logD("Sending command: " + command.getCommand());
		client.onStartSendingCommand(command);
		startNetworkCommand(new SendDeviceCommandCommand(deviceData.getId(), command));
	}

	/* package */void reloadDeviceData(DeviceData deviceData) {
		startNetworkCommand(new GetDeviceCommand(deviceData.getId()));
	}

	/* package */void startReceivingNotifications() {
		if (isReceivingNotifications) {
			stopReceivingNotifications();
		}
		isReceivingNotifications = true;
		handleNextNotification();
	}

	/* package */void stopReceivingNotifications() {
		detachResultReceiver();
		isReceivingNotifications = false;
		isPollRequestInProgress = false;
	}

	/* package */void setClient(DeviceClient client) {
		this.client = client;
	}

	/* package */boolean isReceivingNotifications() {
		return isReceivingNotifications;
	}

	private void handleNotification(final Notification notification) {
		if (client.shouldReceiveNotificationAsynchronously(notification)) {
			asyncHandler.post(new Runnable() {
				@Override
				public void run() {
					didReceiveNotification(notification);
					mainThreadHandler.post(new Runnable() {
						@Override
						public void run() {
							if (isReceivingNotifications) {
								handleNextNotification();
							}
						}
					});
				}
			});
		} else {
			didReceiveNotification(notification);
			if (isReceivingNotifications) {
				handleNextNotification();
			}
		}
	}

	private void handleNextNotification() {
		final Notification notification = notificationQueue.poll();
		if (notification != null) {
			handleNotification(notification);
		} else {
			if (!isPollRequestInProgress) {
				isPollRequestInProgress = true;
				if (lastNotificationPollTimestamp == null) {
					// timestamp wasn't specified. Request and use server
					// timestamp instead.
					logD("Starting Get API info command");
					startNetworkCommand(new GetApiInfoCommand());
				} else {
					startPollNotificationsRequest();
				}
			}
		}
	}

	protected abstract NotificationsRetrivalCommand getPollNotificationsCommand(
			String lastNotificationPollTimestamp, Integer waitTimeout);

	protected abstract void didReceiveNotification(Notification notification);

	private void startPollNotificationsRequest() {
		logD("Starting polling request with lastNotificationPollTimestamp = "
				+ lastNotificationPollTimestamp);
		startNetworkCommand(getPollNotificationsCommand(
				lastNotificationPollTimestamp, notificationPollWaitTimeout));
	}

	private int enqueueNotifications(List<Notification> notifications) {
		if (notifications == null || notifications.isEmpty()) {
			return 0;
		}
		int enqueuedCount = 0;
		for (Notification notification : notifications) {
			boolean added = notificationQueue.offer(notification);
			if (!added) {
				Log.e(DeviceHive.TAG, "Failed to add notification to the queue");
			} else {
				enqueuedCount++;
			}
		}
		return enqueuedCount;
	}

	@Override
	protected void onReceiveResult(final int resultCode, final int tagId,
			final Bundle resultData) {
		switch (resultCode) {
		case DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE:
			logD("Handled response");
			if (tagId == TAG_SEND_COMMAND) {
				Command command = SendDeviceCommandCommand
						.getSentCommand(resultData);
				logD("Command sent with response: " + command);
				client.onFinishSendingCommand(command);
			} else if (tagId == TAG_POLL_NOTIFICATIONS
					|| tagId == TAG_POLL_MULTIPLE_NOTIFICATIONS) {
				logD("Poll request finished");
				isPollRequestInProgress = false;
				List<Notification> notifications = PollDeviceNotificationsCommand
						.getNotifications(resultData);
				logD("-------Received notifications: " + notifications);
				logD("Notifications count: " + notifications.size());
				int enqueuedCount = enqueueNotifications(notifications);
				logD("Enqueued notifications count: " + enqueuedCount);
				if (!notifications.isEmpty()) {
					lastNotificationPollTimestamp = notifications.get(
							notifications.size() - 1).getTimestamp();
				}
				if (isReceivingNotifications) {
					handleNextNotification();
				}
			} else if (tagId == TAG_GET_DEVICE) {
				logD("Get device request finished");
				final DeviceData deviceData = GetDeviceCommand
						.getDevice(resultData);
				client.onReloadDeviceDataFinishedInternal(deviceData);
			} else if (tagId == TAG_GET_API_INFO) {
				final ApiInfo apiInfo = GetApiInfoCommand
						.getApiInfo(resultData);
				logD("Get API info request finished: " + apiInfo);
				lastNotificationPollTimestamp = apiInfo.getServerTimestamp();
				logD("Starting polling request with lastNotificationPollTimestamp = "
						+ lastNotificationPollTimestamp);
				startPollNotificationsRequest();
			}
			break;
		case DeviceHiveResultReceiver.MSG_EXCEPTION:
			final Throwable exception = NetworkCommand.getThrowable(resultData);
			Log.e(DeviceHive.TAG, "DeviceHiveResultReceiver.MSG_EXCEPTION",
					exception);
		case DeviceHiveResultReceiver.MSG_STATUS_FAILURE:
			if (tagId == TAG_SEND_COMMAND) {
				SendDeviceCommandCommand command = (SendDeviceCommandCommand) NetworkCommand
						.getCommand(resultData);
				client.onFailSendingCommand(command.getCommand());
			} else if (tagId == TAG_POLL_NOTIFICATIONS
					|| tagId == TAG_POLL_MULTIPLE_NOTIFICATIONS
					|| tagId == TAG_GET_API_INFO) {
				Log.d(DeviceHive.TAG, "Failed to poll notifications");
				isPollRequestInProgress = false;
				if (isReceivingNotifications) {
					handleNextNotification();
				}
			} else if (tagId == TAG_GET_DEVICE) {
				client.onReloadDeviceDataFailedInternal();
			}
			break;
		}

	}

	@Override
	protected NetworkCommandConfig getCommandConfig() {
		final NetworkCommandConfig config = super.getCommandConfig();
		config.setBasicAuthorisation(username, password);
		return config;
	}

	private final static int TAG_SEND_COMMAND = getTagId(SendDeviceCommandCommand.class);
	private final static int TAG_POLL_NOTIFICATIONS = getTagId(PollDeviceNotificationsCommand.class);
	private final static int TAG_POLL_MULTIPLE_NOTIFICATIONS = getTagId(PollMultipleDeviceNotificationsCommand.class);
	private final static int TAG_GET_DEVICE = getTagId(GetDeviceCommand.class);
	private static final int TAG_GET_API_INFO = getTagId(GetApiInfoCommand.class);
}
