package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.content.Context;

import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.R;
import com.google.gson.Gson;

/**
 * Created by Constantine Mars on 4/8/15.
 *
 * Formatter for json command results
 */
public class CmdResult {
    protected String serviceUUID;
    protected String characteristicUUID;
    protected String device;

    public CmdResult(String serviceUUID, String characteristicUUID, String device, Context context) {
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.device = device;
        this.context = context;
    }

    protected Context context;

    private  String jsonFullStatus(int statusStringId) {
        return jsonFullStatus(context.getString(statusStringId));
    }

    private static String jsonStatusOk() {
        return new Gson().toJson(StatusJson.Status.statusOk());
    }

    private static String jsonStatusOkWithVal(String val) {
        return new Gson().toJson(StatusJson.Status.statusOkWithVal(val));
    }

    private static String jsonStatusWithObject(Object object) {
        return new Gson().toJson(StatusJson.StatusWithObject.statusWithObject(object));
    }

    private static String jsonStatusFail() {
        return new Gson().toJson(StatusJson.Status.statusFail());
    }

    private static String jsonStatusTimeoutReached() {
        return new Gson().toJson(StatusJson.Status.statusTimeoutReached());
    }

    private static String jsonStatus(int strResId){
        return new Gson().toJson(new StatusJson.Status(BTLEApplication.getApplication().getString(strResId)));
    }

    private static String jsonStatus(String status){
        return new Gson().toJson(new StatusJson.Status(status));
    }

    private String jsonFullStatus(String status) {
        return new Gson().toJson(new StatusJson.FullStatus(
                status,
                device,
                serviceUUID,
                characteristicUUID
        ));
    }

    private String jsonFullStatusWithVal(String status, String val) {
        return new Gson().toJson(new StatusJson.FullStatusWithVal(
                status,
                device,
                serviceUUID,
                characteristicUUID,
                val
        ));
    }

    private String jsonFullStatusWithVal(int statusResId, String val) {
        return jsonFullStatusWithVal(BTLEApplication.getApplication().getString(statusResId), val);
    }

    private String jsonFullStatusWithByteArray(String status, byte[] value) {
        return new Gson().toJson(new StatusJson.FullStatusWithByteArray(
                status,
                value,
                device,
                serviceUUID,
                characteristicUUID
        ));
    }

    private String jsonFullStatusWithByteArray(int statusStringId, byte[] value) {
        return jsonFullStatusWithByteArray(context.getString(statusStringId), value);
    }

    public static CommandResult success() {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatusOk());
    }

    public static CommandResult successWithVal(String val) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatusOkWithVal(val));
    }

    public static CommandResult successWithObject(Object object) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonStatusWithObject(object));
    }

    public static CommandResult commandResultFail() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusFail());
    }

    public static CommandResult failWithStatus(String status) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(status));
    }

    protected CommandResult withStatusAndVal(int statusResId, String val) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonFullStatusWithVal(BTLEApplication.getApplication().getString(statusResId), val));
    }

    public static CommandResult failWithStatus(int strResId) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatus(strResId));
    }

    public static CommandResult failTimeoutReached() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonStatusTimeoutReached());
    }

    protected CommandResult sucessFull() {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonFullStatus(R.string.status_json_success));
    }

    protected CommandResult successFullWithVal(String val) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonFullStatusWithVal(R.string.status_json_success, val));
    }

    public CommandResult successFullWithByteArray(byte[] value) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonFullStatusWithByteArray(R.string.status_json_success, value));
    }

    public CommandResult successFullWithStatus(String status) {
        return new CommandResult(CommandResult.STATUS_COMLETED, jsonFullStatus(status));
    }

    protected CommandResult cmdResFullFail() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonFullStatus(R.string.status_json_fail));
    }

    public CommandResult cmdResFullFailStatus(String status) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonFullStatus(status));
    }

    protected CommandResult cmdResFullFailValue(byte[] value) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonFullStatusWithByteArray(R.string.status_json_fail, value));
    }

    protected CommandResult cmdResFullFailStatusAndValue(String status, byte[] value) {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonFullStatusWithByteArray(status, value));
    }

    protected CommandResult cmdResFullNotFound() {
        return new CommandResult(CommandResult.STATUS_FAILED, jsonFullStatus(R.string.status_json_not_found));
    }
}
