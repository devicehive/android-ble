package com.dataart.btle_android.btle_gateway.gateway_helpers;

import android.content.Context;

import com.dataart.btle_android.R;

/**
 * Created by Constantine Mars on 6/14/16.
 * <p>
 * Validation utils
 */

public class ValidationHelper {
    private static final String VALUE_REGEX = "([a-fA-F0-9]{2}){1,}";
    private static final String ADDRESS_REGEX = "(([a-fA-F0-9]{2}:){5})([a-fA-F0-9]{2})";
    private static final String SERVICE_CHARACTERISTIC_UUID_REGEX = "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})|([a-fA-F0-9]{4})";
    private final Context context;

    public ValidationHelper(Context context) {
        this.context = context;
    }

    public String validateAddress(final String command, final String address) {
        return validate(command, R.string.cmd_requires_address, address, ADDRESS_REGEX);
    }

    public String validateServiceUUID(final String command, final String serviceUUID) {
        return validate(command, R.string.cmd_requires_service_uuid, serviceUUID, SERVICE_CHARACTERISTIC_UUID_REGEX);
    }

    public String validateCharacteristicUUID(final String command, final String serviceUUID) {
        return validate(command, R.string.cmd_requires_characteristic_uuid, serviceUUID, SERVICE_CHARACTERISTIC_UUID_REGEX);
    }

    public String validateValue(final String command, final String value) {
        return validate(command, R.string.cmd_requires_value, value, VALUE_REGEX);
    }

    public String validate(final String command, int messageResId, final String value, final String regex) {
        if (value == null || !value.matches(regex)) {
            return context.getString(messageResId, command);
        }

        return null;
    }

    public String validateCharacteristics(final String command, final String address, final String serviceUUID) {
        String v;
        if ((v = validateAddress(command, address)) != null)
            return v;
        else if ((v = validateServiceUUID(command, serviceUUID)) != null)
            return v;
        return null;
    }

    public String validateNotifications(final String command, final String address, final String serviceUUID) {
        String v;
        if ((v = validateAddress(command, address)) != null)
            return v;
        else if ((v = validateServiceUUID(command, serviceUUID)) != null)
            return v;
        return null;
    }

    public String validateRead(final String command, final String address, final String serviceUUID, final String characteristicUUID) {
        String v;
        if((v = validateAddress(command, address)) != null)
            return v;
        else if ((v = validateServiceUUID(command, serviceUUID)) != null)
            return v;
        else if ((v = validateCharacteristicUUID(command, characteristicUUID)) != null)
            return v;
        return null;
    }


    public String validateWrite(String command, String address, String serviceUUID, String characteristicUUID, String value) {
        String v;
        if ((v = validateAddress(command, address)) != null)
            return v;
        else if ((v = validateServiceUUID(command, serviceUUID)) != null)
            return v;
        else if ((v = validateCharacteristicUUID(command, characteristicUUID)) != null)
            return v;
        else if ((v = validateValue(command, value)) != null)
            return v;
        return null;
    }
}
