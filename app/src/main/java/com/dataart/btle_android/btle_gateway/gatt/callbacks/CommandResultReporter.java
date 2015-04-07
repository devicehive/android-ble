package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.content.Context;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.btle_android.R;
import com.google.gson.Gson;

/**
 * Created by Constantine Mars on 4/8/15.
 *
 * Formatter for json command results
 */
public class CommandResultReporter {
    protected String serviceUUID;
    protected String characteristicUUID;
    protected String device;

    public CommandResultReporter(String serviceUUID, String characteristicUUID, String device, Context context) {
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.device = device;
        this.context = context;
    }

    protected Context context;

    private String jsonStatus(int statusStringId) {
        return jsonStatus(context.getString(statusStringId));
    }

    private String jsonStatus(String status) {
        return new Gson().toJson(new StatusJson.Status(
                status,
                device,
                serviceUUID,
                characteristicUUID
        ));
    }

    private String jsonStatusWithValue(String status, byte[] value) {
        return new Gson().toJson(new StatusJson.StatusWithValue(
                status,
                value,
                device,
                serviceUUID,
                characteristicUUID
        ));
    }

    private String jsonStatusWithValue(int statusStringId, byte[] value) {
        return jsonStatusWithValue(context.getString(statusStringId), value);
    }

    protected CommandResult cmdResSuccess() {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatus(R.string.status_json_success));
    }

    protected CommandResult cmdResSuccessValue(byte[] value) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatusWithValue(R.string.status_json_success, value));
    }

    public CommandResult cmdResSuccessStatus(String status) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatus(status));
    }

    protected CommandResult cmdResFail() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(R.string.status_json_fail));
    }

    public CommandResult cmdResFailStatus(String status) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(status));
    }

    protected CommandResult cmdResFailValue(byte[] value) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusWithValue(R.string.status_json_fail, value));
    }

    protected CommandResult cmdResFailStatusAndValue(String status, byte[] value) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusWithValue(status, value));
    }

    protected CommandResult cmdResNotFound() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(R.string.status_json_not_found));
    }
}
