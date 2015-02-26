package com.dataart.android.devicehive.network;

import android.bluetooth.BluetoothClass.Device;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.dataart.android.devicehive.DeviceHive;

/**
 * Abstract base class for service connections. Implements common functionality.
 * Descendants are expected to implement specific communication logic.
 */
public abstract class ServiceConnection {

	protected final Context context;
	private DeviceHiveResultReceiver resultReceiver;

	protected final static Handler mainThreadHandler = new Handler();

	protected final static Handler asyncHandler;

	protected String apiEndpointUrl;
	protected boolean isDebugLoggingEnabled = false;

	static {
		final HandlerThread thread = new HandlerThread(
				Device.class.getSimpleName() + "[Handler Thread]");
		thread.start();
		asyncHandler = new Handler(thread.getLooper());
	}

	private final DeviceHiveResultReceiver.ResultListener resultListener = new DeviceHiveResultReceiver.ResultListener() {
		@Override
		public void onReceiveResult(int code, int tag, Bundle data) {
			ServiceConnection.this.onReceiveResult(code, tag, data);
		}
	};

	public ServiceConnection(Context context) {
		this.context = context;
	}

	public void setApiEndpointUrl(String url) {
		if (apiEndpointUrl != null && !apiEndpointUrl.equals(url)) {
			// detach result listener to avoid receiving responses from old endpoint.
			if (resultReceiver != null) {
				resultReceiver.detachResultListener();
				resultReceiver = null;
			}
		}
		this.apiEndpointUrl = url;
	}

	public String getApiEndpointUrl() {
		return apiEndpointUrl;
	}

	public void setDebugLoggingEnabled(boolean enabled) {
		this.isDebugLoggingEnabled = enabled;
	}

	public Context getContext() {
		return context;
	}

	public void runOnMainThread(Runnable runnable) {
		mainThreadHandler.post(runnable);
	}

	protected void startNetworkCommand(NetworkCommand command) {
		command.start(context, getCommandConfig());
	}

	protected NetworkCommandConfig getCommandConfig() {
		final NetworkCommandConfig config = new NetworkCommandConfig(
				apiEndpointUrl, getResultReceiver(), isDebugLoggingEnabled);
		return config;
	}

	protected DeviceHiveResultReceiver getResultReceiver() {
		if (resultReceiver == null) {
			resultReceiver = new DeviceHiveResultReceiver();
			resultReceiver.setResultListener(resultListener, true);
		}
		return resultReceiver;
	}
	
	protected void detachResultReceiver() {
		if (resultReceiver != null) {
			resultReceiver.detachResultListener();
			resultReceiver = null;
		}
	}

	protected static final int getTagId(final Class<?> tag) {
		return getTagId(tag.getName());
	}

	protected static final int getTagId(final String tag) {
		return DeviceHiveResultReceiver.getIdForTag(tag);
	}

	protected void logD(final String message) {
		if (isDebugLoggingEnabled) {
			Log.d(DeviceHive.TAG, message);
		}
	}

	protected abstract void onReceiveResult(final int resultCode,
			final int tagId, final Bundle resultData);
}
