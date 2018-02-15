package com.dataart.btle_android.btle_gateway.gatt_callbacks;

import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.R;

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

    static class Status {
        private String status;

        Status(String status) {
            this.status = status;
        }

        static Status statusOk() {
            return new Status(BTLEApplication.getApplication().getString(android.R.string.ok));
        }

        public static Status statusOkWithVal(String val) {
            return new Status(val);
        }

        public static Status statusFail() {
            return new Status(BTLEApplication.getApplication().getString(android.R.string.ok));
        }

        public static Status statusFailWithVal(String val) {
            return new Status(val);
        }

        static Status statusTimeoutReached() {
            return new Status(BTLEApplication.getApplication().getString(R.string.status_timeout));
        }

        public String getStatus() {
            return status;
        }
    }

    static class StatusWithObject {
        private Object result;

        StatusWithObject(Object result) {
            this.result = result;
        }

        static StatusWithObject statusWithObject(Object object) {
            return new StatusWithObject(object);
        }
    }

    static class FullStatusWithVal extends FullStatus {
        private String value;

        FullStatusWithVal(String status, String device, String serviceUUID, String characteristicUUID, String value) {
            super(status, device, serviceUUID, characteristicUUID);
            this.value = value;
        }
    }

    static class FullStatus {
        private String status;
        private String device;
        private String serviceUUID;
        private String characteristicUUID;

        public FullStatus(String status, String device, String serviceUUID, String characteristicUUID) {
            this.status = status;
            this.device = device;
            this.serviceUUID = serviceUUID;
            this.characteristicUUID = characteristicUUID;
        }

        public String getDevice() {
            return device;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class FullStatusWithByteArray extends FullStatus {
        private byte[] value;

        public FullStatusWithByteArray(String status, byte[] value, String device, String serviceUUID, String characteristicUUID) {
            super(status, device, serviceUUID, characteristicUUID);
            this.value = value;
        }
    }
}