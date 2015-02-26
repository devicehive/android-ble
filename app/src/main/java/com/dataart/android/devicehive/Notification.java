package com.dataart.android.devicehive;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a device notification, a unit of information dispatched from
 * {@link Client}s.
 */
public class Notification implements Parcelable {
	private int id;
	@SerializedName("notification")
	private String name;
	private String timestamp;
	private ObjectWrapper<Serializable> parameters;

	protected Notification(int id, String name, String timestamp,
			Serializable parameters) {
		this.id = id;
		this.name = name;
		this.timestamp = timestamp;
		this.parameters = new ObjectWrapper<Serializable>(parameters);
	}

	/**
	 * Construct a new notification with given name and parameters.
	 * 
	 * @param name
	 *            Notification name.
	 * @param parameters
	 *            Notification parameters.
	 */
	public Notification(String name, Serializable parameters) {
		this(-1, name, null, parameters);
	}

	/**
	 * Get notification identifier.
	 * 
	 * @return Notification identifier.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get notification name.
	 * 
	 * @return Notification name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get notification timestamp(UTC).
	 * 
	 * @return Notification timestamp(UTC).
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Get notification parameters dictionary.
	 * 
	 * @return Notification parameters dictionary.
	 */
	public Serializable getParameters() {
		return parameters != null ? parameters.getObject() : parameters;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(name);
		dest.writeSerializable(timestamp);
		dest.writeSerializable(parameters != null ? parameters.getObject()
				: parameters);
	}

	public static Creator<Notification> CREATOR = new Creator<Notification>() {

		@Override
		public Notification[] newArray(int size) {
			return new Notification[size];
		}

		@Override
		public Notification createFromParcel(Parcel source) {
			return new Notification(source.readInt(), source.readString(),
					source.readString(), source.readSerializable());
		}
	};

	@Override
	public String toString() {
		return "Notification [id=" + id + ", name=" + name + ", timestamp="
				+ timestamp + ", parameters=" + parameters + "]";
	}

}
