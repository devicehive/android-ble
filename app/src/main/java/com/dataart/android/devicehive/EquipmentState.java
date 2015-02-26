package com.dataart.android.devicehive;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Represents equipment state.
 */
public class EquipmentState implements Parcelable {
	@SerializedName("id")
	private String equipmentCode;
	private String timestamp;
	private ObjectWrapper<Serializable> parameters;

	/**
	 * Construct equipment state with given parameters.
	 * 
	 * @param code
	 *            Equipment code.
	 * @param timestamp
	 * @param parameters
	 */
	/* package */EquipmentState(String code, String timestamp,
			Serializable parameters) {
		this.equipmentCode = code;
		this.timestamp = timestamp;
		this.parameters = new ObjectWrapper<Serializable>(parameters);
	}

	/**
	 * Get equipment code.
	 * 
	 * @return Equipment code value.
	 */
	public String getEquipmentCode() {
		return equipmentCode;
	}

	/**
	 * Get equipment state timestamp.
	 * 
	 * @return Equipment state timestamp.
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Get current equipment state.
	 * 
	 * @return Equipment state parameters.
	 */
	public Object getParameters() {
		return parameters != null ? parameters.getObject() : parameters;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(equipmentCode);
		dest.writeString(timestamp);
		dest.writeSerializable(parameters != null ? parameters.getObject()
				: parameters);
	}

	public static Creator<EquipmentState> CREATOR = new Creator<EquipmentState>() {

		@Override
		public EquipmentState[] newArray(int size) {
			return new EquipmentState[size];
		}

		@Override
		public EquipmentState createFromParcel(Parcel source) {
			return new EquipmentState(source.readString(), source.readString(),
					source.readSerializable());
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public String toString() {
		return String.format(
				"EquipmentState(code = %s, timestamp = %s, parameters = %s)",
				equipmentCode, timestamp, parameters);
	}

}
