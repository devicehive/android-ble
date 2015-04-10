package com.dataart.android.devicehive.device.future;

import com.dataart.android.devicehive.device.CommandResult;

/**
 * Created by Constantine Mars on 4/9/15.
 */
public class CmdResFuture extends SimpleCallableFuture<CommandResult> {
    public CmdResFuture(CommandResult arg) {
        super(arg);
    }

    public CmdResFuture() {
        super(null);
    }
}
