package com.dataart.android.devicehive.device;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.dataart.android.devicehive.ApiInfo;
import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.DeviceData;
import com.dataart.android.devicehive.DeviceHive;
import com.dataart.android.devicehive.Notification;
import com.dataart.android.devicehive.commands.GetApiInfoCommand;
import com.dataart.android.devicehive.device.commands.GetDeviceCommand;
import com.dataart.android.devicehive.device.commands.PollDeviceCommandsCommand;
import com.dataart.android.devicehive.device.commands.RegisterDeviceCommand;
import com.dataart.android.devicehive.device.commands.SendNotificationCommand;
import com.dataart.android.devicehive.device.commands.UpdateCommandStatusCommand;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.dataart.android.devicehive.network.NetworkCommand;
import com.dataart.android.devicehive.network.ServiceConnection;

/* package */class DeviceServiceConnection extends ServiceConnection {

	private final static String EQUIPMENT_PARAMETER = "equipment";

	private Device device;

	private final Queue<Command> commandQueue = new LinkedList<Command>();

	private boolean isProcessingCommands = false;
	private boolean isPollRequestInProgress = false;

	private String lastCommandPollTimestamp;
	private Integer commandPollWaitTimeout;

	public DeviceServiceConnection(Context context) {
		super(context);
	}

	public void setLastCommandPollTimestamp(String timestamp) {
		this.lastCommandPollTimestamp = timestamp;
	}

	/* package */void setCommandPollWaitTimeout(Integer timeout) {
		this.commandPollWaitTimeout = timeout;
	}

	@Override
	public void setApiEndpointUrl(String url) {
		if (apiEndpointUrl != null && !apiEndpointUrl.equals(url)) {
			isPollRequestInProgress = false;
		}
		super.setApiEndpointUrl(url);
	}

	/* package */void sendNotification(Notification notification) {
		logD("Sending notification: " + notification.getName());
		device.onStartSendingNotification(notification);
		final DeviceData deviceData = device.getDeviceData();
		startNetworkCommand(new SendNotificationCommand(deviceData.getId(),
				deviceData.getKey(), notification));
	}

	/* package */void startProcessingCommands() {
		if (isProcessingCommands) {
			stopProcessingCommands();
		}
		isProcessingCommands = true;
		executeNextCommand();
	}

	/* package */void stopProcessingCommands() {
		detachResultReceiver();
		isProcessingCommands = false;
		isPollRequestInProgress = false;
	}

	/* package */void setDevice(Device device) {
		this.device = device;
	}

	/* package */boolean isProcessingCommands() {
		return isProcessingCommands;
	}

	/* package */void registerDevice() {
		startNetworkCommand(new RegisterDeviceCommand(device.getDeviceData()));
	}

	/* package */void unregisterDevice() {
		logD("Unregistering device");
		unregisterEquipment();
	}

	/* package */void reloadDeviceData() {
		final DeviceData deviceData = device.getDeviceData();
		startNetworkCommand(new GetDeviceCommand(deviceData.getId(),
				deviceData.getKey()));
	}

	private void runCommandOnRunner(final CommandRunner commandRunner,
			final Command command) {
		if (commandRunner.shouldRunCommandAsynchronously(command)) {
			asyncHandler.post(new Runnable() {
				@Override
				public void run() {
					final CommandResult result = commandRunner
							.runCommand(command);
					mainThreadHandler.post(new Runnable() {
						@Override
						public void run() {
							updateCommandStatus(command, result);
						}
					});
				}
			});
		} else {
			final CommandResult result = commandRunner.runCommand(command);
			updateCommandStatus(command, result);
		}
	}

	@SuppressWarnings("rawtypes")
	private void executeNextCommand() {
		final Command command = commandQueue.poll();
		if (command != null) {
			device.onBeforeRunCommand(command);
			Object parameters = command.getParameters();
			if (parameters == null || !(parameters instanceof Map)
					|| !((Map) parameters).containsKey(EQUIPMENT_PARAMETER)) {
				runCommandOnRunner(device, command);
			} else {
				Equipment equipment = device
						.getEquipmentWithCode((String) ((Map) parameters)
								.get(EQUIPMENT_PARAMETER));
				if (equipment != null) {
					equipment.onBeforeRunCommand(command);
					runCommandOnRunner(equipment, command);
				} else {
					updateCommandStatus(command, new CommandResult(
							CommandResult.STATUS_FAILED, "Equipment not found"));
				}
			}
		} else {
			if (!isPollRequestInProgress) {
				isPollRequestInProgress = true;
				if (lastCommandPollTimestamp == null) {
					// timestamp wasn't specified. Request and use server
					// timestamp instead.
					logD("Starting Get API info command");
					startNetworkCommand(new GetApiInfoCommand());
				} else {
					startPollCommandsRequest();
				}
			}
		}
	}

	private void startPollCommandsRequest() {
		logD("Starting polling request with lastCommandPollTimestamp = "
				+ lastCommandPollTimestamp);
		final DeviceData deviceData = device.getDeviceData();
		startNetworkCommand(new PollDeviceCommandsCommand(deviceData.getId(),
				deviceData.getKey(), lastCommandPollTimestamp,
				commandPollWaitTimeout));
	}

	private void updateCommandStatus(Command deviceCommand, CommandResult result) {
		logD(String.format("Update command(%s) status(%s) and result(%s)",
				deviceCommand.getCommand(), result.getStatus(),
				result.getResult()));
		final DeviceData deviceData = device.getDeviceData();
		startNetworkCommand(new UpdateCommandStatusCommand(deviceData.getId(),
				deviceData.getKey(), deviceCommand.getId(), result));
	}

	private int enqueueCommands(List<Command> commands) {
		if (commands == null || commands.isEmpty()) {
			return 0;
		}
		int enqueuedCount = 0;
		for (Command command : commands) {
			if (TextUtils.isEmpty(command.getStatus())) {
				boolean added = commandQueue.offer(command);
				if (!added) {
					Log.e(DeviceHive.TAG,
							"Failed to add command to the command queue");
				} else {
					enqueuedCount++;
				}
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
			if (tagId == TAG_REGISTER) {
				DeviceData deviceData = RegisterDeviceCommand
						.getDeviceData(resultData);
				logD("Device registration finished with data: " + deviceData);
				sendNotification(DeviceStatusNotification.STATUS_ONLINE);
			} else if (tagId == TAG_SEND_NOTIFICATION) {
				Notification notification = SendNotificationCommand
						.getNotification(resultData);
				logD("Notification sent with response: " + notification);
				device.onFinishSendingNotification(notification);
				if (!device.isRegistered()) {
					registerEquipment();
				}
			} else if (tagId == TAG_POLL_COMMANDS) {
				logD("Poll request finished");
				isPollRequestInProgress = false;
				List<Command> commands = PollDeviceCommandsCommand
						.getCommands(resultData);
				logD("-------Received commands: " + commands);
				logD("Commands count: " + commands.size());
				int enqueuedCount = enqueueCommands(commands);
				logD("Enqueued commands count: " + enqueuedCount);
				if (!commands.isEmpty()) {
					lastCommandPollTimestamp = commands
							.get(commands.size() - 1).getTimestamp();
				}
				if (isProcessingCommands) {
					executeNextCommand();
				}
			} else if (tagId == TAG_UPDATE_COMMAND_STATUS) {
				Command command = UpdateCommandStatusCommand
						.getUpdatedCommand(resultData);
				logD("Updated command: " + command);
				if (isProcessingCommands) {
					executeNextCommand();
				}
			} else if (tagId == TAG_GET_DEVICE) {
				logD("Get device request finished");
				final DeviceData deviceData = GetDeviceCommand
						.getDevice(resultData);
				device.onReloadDeviceDataFinishedInternal(deviceData);
			} else if (tagId == TAG_GET_API_INFO) {
				final ApiInfo apiInfo = GetApiInfoCommand
						.getApiInfo(resultData);
				logD("Get API info request finished: " + apiInfo);
				lastCommandPollTimestamp = apiInfo.getServerTimestamp();
				startPollCommandsRequest();
			}
			break;
		case DeviceHiveResultReceiver.MSG_EXCEPTION:
			final Throwable exception = NetworkCommand.getThrowable(resultData);
			Log.e(DeviceHive.TAG, "DeviceHiveResultReceiver.MSG_EXCEPTION",
					exception);
		case DeviceHiveResultReceiver.MSG_STATUS_FAILURE:
			if (tagId == TAG_REGISTER) {
				device.onFailRegistration();
			}
			if (tagId == TAG_POLL_COMMANDS || tagId == TAG_GET_API_INFO) {
				isPollRequestInProgress = false;
				if (isProcessingCommands) {
					executeNextCommand();
				}
			} else if (tagId == TAG_UPDATE_COMMAND_STATUS) {
				logD("Failed to update command status");
				// for now skip this command and try to execute next
				if (isProcessingCommands) {
					executeNextCommand();
				}
			} else if (tagId == TAG_SEND_NOTIFICATION) {
				SendNotificationCommand command = (SendNotificationCommand) NetworkCommand
						.getCommand(resultData);
				Notification notification = command.getNotification();
				device.onFailSendingNotification(notification);
				if (!device.isRegistered()) {
					device.onFailRegistration();
				}
			} else if (tagId == TAG_GET_DEVICE) {
				device.onReloadDeviceDataFailedInternal();
			}
			break;
		}

	}

	private final Runnable registerEquipmentRunnable = new Runnable() {
		@Override
		public void run() {
			boolean equipmentRegistrationResult = true;
			for (Equipment eq : device.getEquipment()) {
				equipmentRegistrationResult = equipmentRegistrationResult
						&& eq.onRegisterEquipment();
				if (!equipmentRegistrationResult) {
					break;
				}
			}
			final boolean result = equipmentRegistrationResult;
			mainThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					device.equipmentRegistrationFinished(result);
				}
			});
		}
	};

	private final Runnable unregisterEquipmentRunnable = new Runnable() {
		@Override
		public void run() {
			boolean equipmentUnregistrationResult = true;
			for (Equipment eq : device.getEquipment()) {
				equipmentUnregistrationResult = equipmentUnregistrationResult
						&& eq.onUnregisterEquipment();
				if (!equipmentUnregistrationResult) {
					break;
				}
			}
			final boolean result = equipmentUnregistrationResult;
			mainThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					device.equipmentUnregistrationFinished(result);
				}
			});
		}
	};

	private void registerEquipment() {
		if (device.performsEquipmentRegistrationCallbacksAsynchronously()) {
			asyncHandler.post(registerEquipmentRunnable);
		} else {
			registerEquipmentRunnable.run();
		}
	}

	private void unregisterEquipment() {
		if (device.performsEquipmentRegistrationCallbacksAsynchronously()) {
			asyncHandler.post(unregisterEquipmentRunnable);
		} else {
			unregisterEquipmentRunnable.run();
		}
	}

	private final static int TAG_REGISTER = getTagId(RegisterDeviceCommand.class);
	private final static int TAG_SEND_NOTIFICATION = getTagId(SendNotificationCommand.class);
	private final static int TAG_POLL_COMMANDS = getTagId(PollDeviceCommandsCommand.class);
	private final static int TAG_UPDATE_COMMAND_STATUS = getTagId(UpdateCommandStatusCommand.class);
	private final static int TAG_GET_DEVICE = getTagId(GetDeviceCommand.class);
	private static final int TAG_GET_API_INFO = getTagId(GetApiInfoCommand.class);
}
