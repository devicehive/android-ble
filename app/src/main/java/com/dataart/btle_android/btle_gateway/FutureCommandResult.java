package com.dataart.btle_android.btle_gateway;

import com.dataart.android.devicehive.device.CommandResult;

/**
 * Created by Constantine Mars on 3/31/15.
 */
public class FutureCommandResult {
    public void setResult(CommandResult result) {
        this.result = result;
    }

    public CommandResult getResult() {
        return result;
    }

    CommandResult result = new CommandResult(CommandResult.STATUS_COMLETED, "Ok");
}
