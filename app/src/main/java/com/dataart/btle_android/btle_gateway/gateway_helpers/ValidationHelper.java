package com.dataart.btle_android.btle_gateway.gateway_helpers;

import android.content.Context;

import com.dataart.android.devicehive.device.future.CmdResFuture;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.gatt_callbacks.CmdResult;
import com.google.common.base.Optional;

import lombok.RequiredArgsConstructor;

/**
 * Created by Constantine Mars on 6/14/16.
 * <p>
 * Validation utils
 */

@RequiredArgsConstructor
public class ValidationHelper {
    public static final String VALUE_REGEX = "([a-fA-F0-9]{2}){1,}";
    public static final String ADDRESS_REGEX = "(([a-fA-F0-9]{2}:){5})([a-fA-F0-9]{2})";
    public static final String SERVICE_CHARACTERISTIC_UUID_REGEX = "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})|([a-fA-F0-9]{4})";
    private final Context context;

    public Optional<CmdResFuture> validateAddress(final String command, final String address) {
        return validate(command, R.string.cmd_requires_address, address, ADDRESS_REGEX);
    }

    public Optional<CmdResFuture> validateServiceUUID(final String command, final String serviceUUID) {
        return validate(command, R.string.cmd_requires_service_uuid, serviceUUID, SERVICE_CHARACTERISTIC_UUID_REGEX);
    }

    public Optional<CmdResFuture> validateCharacteristicUUID(final String command, final String serviceUUID) {
        return validate(command, R.string.cmd_requires_characteristic_uuid, serviceUUID, SERVICE_CHARACTERISTIC_UUID_REGEX);
    }

    public Optional<CmdResFuture> validateValue(final String command, final String value) {
        return validate(command, R.string.cmd_requires_value, value, VALUE_REGEX);
    }

    public Optional<CmdResFuture> validate(final String command, int messageResId, final String value, final String regex) {
        if (value == null || !value.matches(regex)) {
            return Optional.of(new CmdResFuture(
                    CmdResult.failWithStatus(context.getString(messageResId, command))
            ));
        }

        return Optional.absent();
    }

    public Optional<CmdResFuture> validateCharacteristics(final String command, final String address, final String serviceUUID) {
        return validateAddress(command, address)
                .or(validateServiceUUID(command, serviceUUID));
    }

    public Optional<CmdResFuture> validateNotifications(final String command, final String address, final String serviceUUID) {
        return validateAddress(command, address)
                .or(validateServiceUUID(command, serviceUUID));
    }

    public Optional<CmdResFuture> validateRead(final String command, final String address, final String serviceUUID, final String characteristicUUID) {
        return validateAddress(command, address)
                .or(validateServiceUUID(command, serviceUUID))
                .or(validateCharacteristicUUID(command, characteristicUUID));
    }


    public Optional<CmdResFuture> validateWrite(String command, String address, String serviceUUID, String characteristicUUID, String value) {
        return validateAddress(command, address)
                .or(validateServiceUUID(command, serviceUUID))
                .or(validateCharacteristicUUID(command, characteristicUUID))
                .or(validateValue(command, value));
    }
}
