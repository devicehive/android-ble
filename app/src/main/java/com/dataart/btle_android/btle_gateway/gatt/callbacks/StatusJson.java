package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.R;

/**
 * Created by Constantine Mars on 4/3/15.
 *
 * Wraps data for json conversion
 */
public abstract class StatusJson {
    public static class Status {
        private String status;

        public Status(String status) {
            this.status = status;
        }

        public static Status statusOk() {
            return new Status(BTLEApplication.getApplication().getString(R.string.status_ok));
        }

        public static Status statusOkWithVal(String val) {
            return new Status(val);
        }

        public static Status statusFail() {
            return new Status(BTLEApplication.getApplication().getString(R.string.status_ok));
        }

        public static Status statusFailWithVal(String val) {
            return new Status(val);
        }

        public static Status statusTimeoutReached() {
            return new Status(BTLEApplication.getApplication().getString(R.string.status_timeout));
        }

        public String getStatus() {
            return status;
        }
    }

    public static class FullStatusWithVal extends FullStatus {
        private String value;

        public FullStatusWithVal(String status, String device, String serviceUUID, String characteristicUUID, String value) {
            super(status, device, serviceUUID, characteristicUUID);
            this.value = value;
        }
    }

    public static class FullStatus {
        private String status;
        private String device;
        private String serviceUUID;

        public String getDevice() {
            return device;
        }

        public String getStatus() {
            return status;
        }

        private String characteristicUUID;

        public FullStatus(String status, String device, String serviceUUID, String characteristicUUID) {
            this.status = status;
            this.device = device;
            this.serviceUUID = serviceUUID;
            this.characteristicUUID = characteristicUUID;
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