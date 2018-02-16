package com.dataart.btle_android.btle_gateway.gatt_callbacks;

import android.content.Context;

/**
 * Created by Constantine Mars on 4/8/15.
 *
 * Formatter for json command results
 */
public class CmdResult {
    protected String serviceUUID;
    protected String characteristicUUID;
    protected final String device;
    protected final Context context;

    public CmdResult(String serviceUUID, String characteristicUUID, String device, Context context) {
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.device = device;
        this.context = context;
    }
}
