package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.R;

/**
 * Created by Constantine Mars on 4/3/15.
 */
public abstract class StatusJson {
    public static class Status implements Parcelable {
        private String status;

        public Status(String status) {
            this.status = status;
        }

        public static Status statusOk() {
            return new Status(BTLEApplication.getApplication().getString(R.string.status_ok));
        }

        public static Status statusFail() {
            return new Status(BTLEApplication.getApplication().getString(R.string.status_ok));
        }

        public static Status statusTimeoutReached() {
            return new Status(BTLEApplication.getApplication().getString(R.string.status_timeout));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(status);
        }
    }

    public static class FullStatus implements Parcelable {
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

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(status);
            parcel.writeString(device);
            parcel.writeString(serviceUUID);
            parcel.writeString(characteristicUUID);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    public static class FullStatusWithValue extends FullStatus {
        private byte[] value;

        public FullStatusWithValue(String status, byte[] value, String device, String serviceUUID, String characteristicUUID) {
            super(status, device, serviceUUID, characteristicUUID);
            this.value = value;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeByteArray(value);
        }
    }
}