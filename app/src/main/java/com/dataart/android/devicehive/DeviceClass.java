package com.dataart.android.devicehive;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a device class which holds meta-information about {@link Client}s.
 */
public class DeviceClass extends DataContainer {
	private int id;
	private String name;
	private String version;
	private boolean isPermanent;
	private Integer offlineTimeout;

	/**
	 * Construct a new device class with given parameters. This method is
	 * intended for internal use.
	 * 
	 * @param id
	 *            Device class identifier.
	 * @param name
	 *            Device class display name.
	 * @param version
	 *            Device class version.
	 * @param isPermanent
	 *            Whether this device class is permanent.
	 * @param offlineTimeout
	 *            If set, specifies inactivity timeout in seconds before the
	 *            framework changes device status to "Offline".
	 */
	public DeviceClass(int id, String name, String version,
			boolean isPermanent, Integer offlineTimeout) {
		this(null, id, name, version, isPermanent, offlineTimeout);
	}

	/* package */DeviceClass(Serializable data, int id, String name,
			String version, boolean isPermanent, Integer offlineTimeout) {
		super(data);
		this.id = id;
		this.name = name;
		this.version = version;
		this.isPermanent = isPermanent;
		this.offlineTimeout = offlineTimeout;
	}

	/**
	 * Construct a new device class with given parameters.
	 * 
	 * @param name
	 *            Device class display name.
	 * @param version
	 *            Device class version.
	 * @param isPermanent
	 *            Whether this device class is permanent.
	 * @param offlineTimeout
	 *            If set, specifies inactivity timeout in seconds before the
	 *            framework changes device status to "Offline".
	 */
	public DeviceClass(String name, String version, boolean isPermanent,
			Integer offlineTimeout) {
		this(-1, name, version, isPermanent, offlineTimeout);
	}

	/**
	 * Constructs a new device class with given parameters.
	 * 
	 * @param name
	 *            Device class display name.
	 * @param version
	 *            Device class version.
	 */
	public DeviceClass(String name, String version) {
		this(-1, name, version, false, null);
	}

	/**
	 * Get device class identifier.
	 * 
	 * @return Device class identifier.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get device class display name.
	 * 
	 * @return Device class display name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get device class version.
	 * 
	 * @return Device class version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Check whether the device class is permanent. Permanent device classes
	 * could not be modified by devices during registration.
	 * 
	 * @return true if device class is permanent, otherwise returns false.
	 */
	public boolean isPermanent() {
		return isPermanent;
	}

	/**
	 * Get offline timeout. If set, specifies inactivity timeout in seconds
	 * before the framework changes device status to "Offline".
	 * 
	 * @return Inactivity timeout value in seconds.
	 */
	public Integer getOfflineTimeout() {
		return offlineTimeout;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeInt(id);
		dest.writeString(name);
		dest.writeString(version);
		dest.writeInt(isPermanent ? 1 : 0);
		dest.writeValue(offlineTimeout);
	}

	public static Parcelable.Creator<DeviceClass> CREATOR = new Parcelable.Creator<DeviceClass>() {

		@Override
		public DeviceClass[] newArray(int size) {
			return new DeviceClass[size];
		}

		@Override
		public DeviceClass createFromParcel(Parcel source) {
			return new DeviceClass(source.readSerializable(), source.readInt(),
					source.readString(), source.readString(),
					source.readInt() > 0, (Integer) source.readValue(null));
		}
	};

	@Override
	public String toString() {
		return "DeviceClass [id=" + id + ", name=" + name + ", version="
				+ version + ", isPermanent=" + isPermanent
				+ ", offlineTimeout=" + offlineTimeout + ", data=" + data + "]";
	}

}
