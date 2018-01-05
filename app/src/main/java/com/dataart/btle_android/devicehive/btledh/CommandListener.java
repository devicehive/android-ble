package com.dataart.btle_android.devicehive.btledh;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;
import com.github.devicehive.client.service.DeviceCommand;

interface CommandListener {
    SimpleCallableFuture<CommandResult> onDeviceReceivedCommand(DeviceCommand command);
}
