package com.dataart.btle_android.devicehive.btledh;

import com.github.devicehive.client.service.DeviceCommand;

public interface CommandListener {
    SimpleCallableFuture<CommandResult> onDeviceReceivedCommand(DeviceCommand command);
}
