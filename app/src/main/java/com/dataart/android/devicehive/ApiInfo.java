package com.dataart.android.devicehive;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents meta-information about the current API.
 */
public class ApiInfo implements Parcelable {
	private String apiVersion;
	private String serverTimestamp;
	private String webSocketServerUrl;

	/**
	 * Constructs API info object with given parameters.
	 * 
	 * @param apiVersion
	 *            API version.
	 * @param serverTimestamp
	 *            Current server timestamp.
	 * @param webSocketServerUrl
	 *            WebSocket server URL.
	 */
	public ApiInfo(String apiVersion, String serverTimestamp,
			String webSocketServerUrl) {
		this.apiVersion = apiVersion;
		this.serverTimestamp = serverTimestamp;
		this.webSocketServerUrl = webSocketServerUrl;
	}

	/**
	 * Get API version.
	 * 
	 * @return API version string.
	 */
	public String getApiVersion() {
		return apiVersion;
	}

	/**
	 * Get current server timestamp.
	 * 
	 * @return Network key.
	 */
	public String getServerTimestamp() {
		return serverTimestamp;
	}

	/**
	 * Get WebSocket server URL.
	 * 
	 * @return WebSocket server URL.
	 */
	public String getWebSocketServerUrl() {
		return webSocketServerUrl;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(apiVersion);
		dest.writeString(serverTimestamp);
		dest.writeString(webSocketServerUrl);
	}

	public static Creator<ApiInfo> CREATOR = new Creator<ApiInfo>() {

		@Override
		public ApiInfo[] newArray(int size) {
			return new ApiInfo[size];
		}

		@Override
		public ApiInfo createFromParcel(Parcel source) {
			return new ApiInfo(source.readString(), source.readString(),
					source.readString());
		}
	};

	@Override
	public String toString() {
		return "ApiInfo [apiVersion=" + apiVersion + ", serverTimestamp="
				+ serverTimestamp + ", webSocketServerUrl="
				+ webSocketServerUrl + "]";
	}
}
