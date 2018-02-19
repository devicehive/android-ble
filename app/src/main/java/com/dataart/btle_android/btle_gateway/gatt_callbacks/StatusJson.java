package com.dataart.btle_android.btle_gateway.gatt_callbacks;

/**
 * Created by Constantine Mars on 4/3/15.
 *
 * Wraps data for json conversion
 */
abstract class StatusJson {
    static String bytes2String(byte[] value) {
        StringBuilder s = new StringBuilder();
        for (byte b : value) {
            if (s.length() > 0) {
                s.append(", ");
            }
            s.append(String.format("0x%02X", b));
        }
        return s.toString();
    }
}
