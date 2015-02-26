package com.dataart.android.devicehive.commands;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.ApiInfo;
import com.dataart.android.devicehive.network.DeviceHiveResultReceiver;
import com.dataart.android.devicehive.network.JsonNetworkCommand;
import com.google.gson.Gson;

/**
 * Gets meta-information of the current API. As a result returns {@link ApiInfo}
 * instance.
 */
public class GetApiInfoCommand extends JsonNetworkCommand {

	private final static String NAMESPACE = GetApiInfoCommand.class.getName();

	private static final String API_INFO_KEY = NAMESPACE
			.concat(".API_INFO_KEY");

	/**
	 * Construct a new command.
	 */
	public GetApiInfoCommand() {

	}

	@Override
	protected String toJson(Gson gson) {
		return null;
	}

	@Override
	protected RequestType getRequestType() {
		return RequestType.GET;
	}

	@Override
	protected String getRequestPath() {
		return "info";
	}

	public static Creator<GetApiInfoCommand> CREATOR = new Creator<GetApiInfoCommand>() {

		@Override
		public GetApiInfoCommand[] newArray(int size) {
			return new GetApiInfoCommand[size];
		}

		@Override
		public GetApiInfoCommand createFromParcel(Parcel source) {
			return new GetApiInfoCommand();
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

	}

	@Override
	protected int fromJson(final String response, final Gson gson,
			final Bundle resultData) {

		final ApiInfo apiInfo = gson.fromJson(response, ApiInfo.class);
		resultData.putParcelable(API_INFO_KEY, apiInfo);
		return DeviceHiveResultReceiver.MSG_HANDLED_RESPONSE;
	}

	/**
	 * Get {@link ApiInfo} object from response {@link android.os.Bundle} container.
	 * 
	 * @param resultData
	 *            {@link android.os.Bundle} object containing required response data.
	 * @return {@link ApiInfo} instance.
	 */
	public final static ApiInfo getApiInfo(Bundle resultData) {
		return resultData.getParcelable(API_INFO_KEY);
	}

}
