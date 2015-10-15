package com.dataart.android.devicehive;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents device data.
 */
public class DeviceData extends DataContainer {

	/**
	 * "Online" device status.
	 */
	public static final String DEVICE_STATUS_ONLINE = "Online";

	/**
	 * "OK" device status.
	 */
	public static final String DEVICE_STATUS_OK = "OK";

	private String id;
	private String key;
	private String name;
	private String status;
	private DeviceClass deviceClass;

	private final static ClassLoader CLASS_LOADER = DeviceData.class
			.getClassLoader();

	/**
	 * Construct device data with given parameters.
	 * 
	 * @param id
	 *            Device unique identifier.
	 * @param key
	 *            Device authentication key. The key maximum length is 64
	 *            characters.
	 * @param name
	 *            Device display name.
	 * @param status
	 *            Device operation status.
	 * @param deviceClass
	 *            Associated {@link DeviceClass}.
	 */
	public DeviceData(String id, String key, String name, String status,
			DeviceClass deviceClass) {
		this(null, id, key, name, status, deviceClass);
	}

	/* package */DeviceData(Serializable data, String id, String key,
			String name, String status,
			DeviceClass deviceClass) {
		super(data);
		this.id = id;
		this.key = key;
		this.name = name;
		this.status = status;
		this.deviceClass = deviceClass;
//		// FIXME: 10/15/15 netowrk and equipment are disabled since october update for DeviceHive Playground
	}

	/**
	 * Get device identifier.
	 *
	 * @return Device identifier.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get device authentication key.
	 * 
	 * @return Device authentication key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get device display name.
	 * 
	 * @return Device display name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get device status.
	 * 
	 * @return Device status.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Get device class.
	 * 
	 * @return Associated {@link DeviceClass} object.
	 */
	public DeviceClass getDeviceClass() {
		return deviceClass;
	}

	/**
	 * Add equipment to the device.
	 *
	 * @param equipmentData
	 *            Equipment to be added.
	 */
	public void addEquipment(EquipmentData equipmentData) {
//		// FIXME: 10/15/15 empty stub
    }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(id);
		dest.writeString(key);
		dest.writeString(name);
		dest.writeString(status);
		dest.writeParcelable(deviceClass, 0);
	}

	public static Parcelable.Creator<DeviceData> CREATOR = new Parcelable.Creator<DeviceData>() {

		@Override
		public DeviceData[] newArray(int size) {
			return new DeviceData[size];
		}

		@Override
		public DeviceData createFromParcel(Parcel source) {
			Serializable data = source.readSerializable();
			String id = source.readString();
			String key = source.readString();
			String name = source.readString();
			String status = source.readString();
			DeviceClass deviceClass = source.readParcelable(CLASS_LOADER);
			return new DeviceData(data, id, key, name, status, deviceClass);
		}
	};

	@Override
	public String toString() {
		return "DeviceData [id=" + id + ", key=" + key + ", name=" + name
				+ ", status=" + status
				+ ", deviceClass=" + deviceClass
				+ ", data=" + data + "]";
	}

}
