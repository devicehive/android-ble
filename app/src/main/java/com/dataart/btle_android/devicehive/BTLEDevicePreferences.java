package com.dataart.btle_android.devicehive;

import android.content.Context;
import android.content.SharedPreferences;

public class BTLEDevicePreferences {

	private final static String NAMESPACE = "devicehive.";

	private final Context context;
	private final SharedPreferences preferences;
	
	private final static String KEY_SERVER_URL = NAMESPACE
			.concat(".KEY_SERVER_URL");

	public BTLEDevicePreferences(final Context context) {
		this.context = context;
		this.preferences = context.getSharedPreferences(
				context.getPackageName() + "_devicehiveprefs",
				Context.MODE_PRIVATE);
	}
	
	public String getServerUrl() {
		return preferences.getString(KEY_SERVER_URL, null);
	}
	
	public void setServerUrlSync(String serverUrl) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(KEY_SERVER_URL, serverUrl);
		editor.commit();
	}
}
