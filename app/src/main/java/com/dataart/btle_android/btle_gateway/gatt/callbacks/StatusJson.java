package com.dataart.btle_android.btle_gateway.gatt.callbacks;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Constantine Mars on 4/3/15.
 */
public abstract class StatusJson {
    public static class Status implements Parcelable {
        private String status;
        private String device;
        private String serviceUUID;
        private String characteristicUUID;

        public Status(String status, String device, String serviceUUID, String characteristicUUID) {
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

    public static class StatusWithValue extends Status {
        private byte[] value;

        public StatusWithValue(String status, byte[] value, String device, String serviceUUID, String characteristicUUID) {
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