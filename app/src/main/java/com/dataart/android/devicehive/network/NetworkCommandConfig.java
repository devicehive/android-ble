package com.dataart.android.devicehive.network;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

import com.dataart.btle_android.BTLEApplication;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

/**
 * Network command execution configuration data.
 */
public class NetworkCommandConfig implements Parcelable {

	/**
	 * Server URL without path component.
	 */
	public final String baseUrl;

	/**
	 * Result receiver to receive messages from the service.
	 */
	public final ResultReceiver resultReceiver;

	/**
	 * Whether debug logging is enabled or not.
	 */
	public final boolean isDebugLoggingEnabled;

	private String username;
	private String password;

	private static final ClassLoader CLASS_LOADER = NetworkCommandConfig.class
			.getClassLoader();

	/* package */NetworkCommandConfig(String baseUrl,
			ResultReceiver resultReceiver,
			boolean isDebugLoggingEnabled, String username, String password) {
		this.baseUrl = baseUrl;
		this.resultReceiver = resultReceiver;
		this.isDebugLoggingEnabled = isDebugLoggingEnabled;
		this.username = username;
		this.password = password;

        BTLEDevicePreferences preferences = new BTLEDevicePreferences();
        this.username = preferences.getUsername();
        this.password = preferences.getPassword();


//        //TODO: check this
//        this.username = "su";
//        this.password = "asdASDqwe123";
	}

	/**
	 * Construct {@link NetworkCommandConfig} instance and initialize it with
	 * given parameters.
	 * 
	 * @param baseUrl
	 *            Server URL excluding path component.
	 * @param resultReceiver
	 *            {@link DeviceHiveResultReceiver} instance to receive messages
	 *            from the service.
	 * @param isDebugLoggingEnabled
	 *            Whether debug logging enabled.
	 */
	public NetworkCommandConfig(String baseUrl,
			DeviceHiveResultReceiver resultReceiver,
			boolean isDebugLoggingEnabled) {
		this(baseUrl, resultReceiver, isDebugLoggingEnabled, null, null);
	}

	/**
	 * Construct {@link NetworkCommandConfig} instance and initialize it with
	 * given parameters.
	 * 
	 * @param baseUrl
	 *            Server URL excluding path component.
	 * @param resultReceiver
	 *            {@link DeviceHiveResultReceiver} instance to receive messages
	 *            from the service.
	 */
	public NetworkCommandConfig(String baseUrl,
			DeviceHiveResultReceiver resultReceiver) {
		this(baseUrl, resultReceiver, false);
	}

	/**
	 * Set Basic Authorization parameters.
	 * 
	 * @param username
	 *            Username string.
	 * @param password
	 *            Password string.
	 */
	public void setBasicAuthorisation(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Get Basic Authorization parameters.
	 * 
	 * @return {@link org.apache.http.auth.UsernamePasswordCredentials} instance containing Basic
	 *         Authorization data.
	 */
	public UsernamePasswordCredentials getBasicAuthorisation() {
		if (username != null && password != null) {
			return new UsernamePasswordCredentials(username, password);
		}
		return null;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(baseUrl);
		dest.writeParcelable(resultReceiver, 0);
		dest.writeInt(isDebugLoggingEnabled ? 1 : 0);
		dest.writeString(username);
		dest.writeString(password);
	}

	public static final Creator<NetworkCommandConfig> CREATOR = new Creator<NetworkCommandConfig>() {
		public NetworkCommandConfig createFromParcel(Parcel source) {
			return new NetworkCommandConfig(source.readString(),
					(ResultReceiver) source
							.readParcelable(CLASS_LOADER),
					source.readInt() > 0, source.readString(),
					source.readString());
		}

		public NetworkCommandConfig[] newArray(int size) {
			return new NetworkCommandConfig[size];
		}
	};

}
