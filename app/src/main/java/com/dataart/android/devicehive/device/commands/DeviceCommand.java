package com.dataart.android.devicehive.device.commands;

import java.util.Map;

import android.os.Parcel;

import com.dataart.android.devicehive.network.JsonNetworkCommand;

/**
 * Base command for Device Hive device-related commands.
 */
public abstract class DeviceCommand extends JsonNetworkCommand {

	protected final String deviceId;
	protected final String deviceKey;

	/**
	 * Construct new command with given device data.
	 * 
	 * @param deviceId
	 *            Device identifier.
	 * @param deviceKey
	 * 			  Device key.
	 */
	public DeviceCommand(String deviceId, String deviceKey) {
		this.deviceId = deviceId;
		this.deviceKey = deviceKey;
	}
	
	/**
	 * Construct new command with given device data.
	 * 
	 * @param deviceId
	 *            Device identifier.
	 */
	public DeviceCommand(String deviceId) {
		this(deviceId, null);
	}

	protected boolean deviceAuthenticationRequired() {
		return true;
	}

	@Override
	protected Map<String, String> getHeaders() {
		final Map<String, String> headers = super.getHeaders();
		if (deviceAuthenticationRequired()) {
			addDeviceAuthentication(headers);
		}
		return headers;
	}

	protected String getEncodedDeviceId() {
		return encodedString(deviceId);
	}

	private void addDeviceAuthentication(Map<String, String> headers) {
//		headers.put("Auth-DeviceID", deviceId);
//		headers.put("Auth-DeviceKey", deviceKey);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(deviceId);
		dest.writeString(deviceKey);
	}

}
