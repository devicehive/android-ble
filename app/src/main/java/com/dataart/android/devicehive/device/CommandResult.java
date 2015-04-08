package com.dataart.android.devicehive.device;

import java.io.Serializable;

import com.dataart.android.devicehive.ObjectWrapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.os.Parcel;
import android.os.Parcelable;

import timber.log.Timber;

/**
 * Command execution result which is reported to the server.
 */
public class CommandResult implements Parcelable {

	/**
	 * Command status "Completed" value.
	 */
	public static final String STATUS_COMLETED = "Completed";

	/**
	 * Command status "Failed" value.
	 */
	public static final String STATUS_FAILED = "Failed";

	/**
	 * Command status "Failed" value.
	 */
	public static final String STATUS_WAITING = "Waiting";

	private final String status;
	private final String result;

	/**
	 * Constructs command result with given status and result.
	 * 
	 * @param status
	 *            Command status.
	 * @param result
	 *            Command execution result.
	 */
	public CommandResult(String status, String result) {
		this.status = status;
		this.result = result;
		Timber.d("CommandResult constructor: r="+result+", this.r="+this.result);
	}

	/**
	 * Get command status.
	 * 
	 * @return Command status.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Get command execution result.
	 * 
	 * @return Command execution result.
	 */
	public Serializable getResult() {
		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(status);
		dest.writeString(result);
	}

	public static Creator<CommandResult> CREATOR = new Creator<CommandResult>() {

		@Override
		public CommandResult[] newArray(int size) {
			return new CommandResult[size];
		}

		@Override
		public CommandResult createFromParcel(Parcel source) {
			return new CommandResult(source.readString(),
					source.readString());
		}
	};

	public String toJson() {
//		FIXME: should be implemented with serializable or custom gson serializer
		String json = "{\"result\":"+ result +",\"status\":\""+status+"\"}";
		return json;
	}

	public static boolean isValidJson(String json){
		Gson gson = new Gson();
		try {
			Object o = gson.fromJson(json, Object.class);
			String reversed = new GsonBuilder().setPrettyPrinting().create().toJson(o);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
