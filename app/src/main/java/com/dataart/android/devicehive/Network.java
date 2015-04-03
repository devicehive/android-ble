package com.dataart.android.devicehive;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a network, an isolated area where {@link Client}s reside.
 */
public class Network implements Parcelable {
	private String key;
	private String name;
	private String description;

	public Network(String key, String name, String description) {
		this.key = key;
		this.name = name;
		this.description = description;
	}

	/**
	 * Constructs network object with given name and description.
	 * 
	 * @param name
	 *            Network display name.
	 * @param description
	 *            Network description.
	 */
	public Network(String name, String description) {
		this(null, name, description);
	}

	/**
	 * Get network key. Optional key that is used to protect the network from
	 * unauthorized device registrations.
	 * 
	 * @return Network key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get network display name.
	 * 
	 * @return Network display name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get network description.
	 * 
	 * @return Network description.
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeString(name);
		dest.writeString(description);
	}

	public static Creator<Network> CREATOR = new Creator<Network>() {

		@Override
		public Network[] newArray(int size) {
			return new Network[size];
		}

		@Override
		public Network createFromParcel(Parcel source) {
			return new Network(source.readString(),
					source.readString(), source.readString());
		}
	};

	@Override
	public String toString() {
		return "Network [key=" + key + ", name=" + name
				+ ", description=" + description + "]";
	}

}
