package com.dataart.btle_android.btle_gateway;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alrybakov
 */

public class BTLEDevice implements Parcelable {

    private String name;
    private String address;

    public BTLEDevice(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
    }

    public static final Parcelable.Creator<BTLEDevice> CREATOR
            = new Parcelable.Creator<BTLEDevice>() {
        public BTLEDevice createFromParcel(Parcel in) {
            return new BTLEDevice(in.readString(), in.readString());
        }

        public BTLEDevice[] newArray(int size) {
            return new BTLEDevice[size];
        }
    };

}
