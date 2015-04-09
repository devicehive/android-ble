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

    private static String jsonStatusOk() {
        return new Gson().toJson(StatusJson.Status.statusOk());
    }

    private static String jsonStatusFail() {
        return new Gson().toJson(StatusJson.Status.statusFail());
    }

    private static String jsonStatusFailWithVal(String val) {
        return new Gson().toJson(StatusJson.Status.statusFailWithVal(val));
    }

    private static String jsonStatusTimeoutReached() {
        return new Gson().toJson(StatusJson.Status.statusTimeoutReached());
    }

    private String jsonStatus(String status) {
        return new Gson().toJson(new StatusJson.FullStatus(
                status,
                device,
                serviceUUID,
                characteristicUUID
        ));
    }

    private String jsonStatusWithValue(String status, byte[] value) {
        return new Gson().toJson(new StatusJson.FullStatusWithValue(
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

    public static CommandResult commandResultSuccess() {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatusOk());
    }

    public static CommandResult commandResultFail() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusFail());
    }

    public static CommandResult commandResultFailWithVal(String val) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusFailWithVal(val));
    }

    public static CommandResult commandResultTimeoutReached() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusTimeoutReached());
    }

    protected CommandResult cmdResFullSuccess() {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatus(R.string.status_json_success));
    }

    protected CommandResult cmdResFullSuccessValue(byte[] value) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatusWithValue(R.string.status_json_success, value));
    }

    public CommandResult cmdResFullSuccessStatus(String status) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatus(status));
    }

    protected CommandResult cmdResFullFail() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(R.string.status_json_fail));
    }

    public CommandResult cmdResFullFailStatus(String status) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(status));
    }

    protected CommandResult cmdResFullFailValue(byte[] value) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusWithValue(R.string.status_json_fail, value));
    }

    protected CommandResult cmdResFullFailStatusAndValue(String status, byte[] value) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusWithValue(status, value));
    }

    protected CommandResult cmdResFullNotFound() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(R.string.status_json_not_found));
    }
}
