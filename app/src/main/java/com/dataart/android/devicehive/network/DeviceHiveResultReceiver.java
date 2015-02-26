package com.dataart.android.devicehive.network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ResultReceiver;
import android.util.Log;

import com.dataart.android.devicehive.DeviceHive;

/**
 * Result receiver which is used to communicate(receive results) with service.
 */
public class DeviceHiveResultReceiver extends ResultReceiver {

	/**
	 * Message with this code is sent when request is started.
	 */
	public final static int MSG_EXECUTE_REQUEST = 0x00000010;

	/**
	 * Message with this code is sent to the result receiver when request is
	 * finished regardless of whether it succeeds or fails.
	 */
	public final static int MSG_COMPLETE_REQUEST = 0x00000011;

	/**
	 * Message with this code is sent to the result receiver when response of
	 * the request is handled(parsed).
	 */
	public final static int MSG_HANDLED_RESPONSE = 0x00000012;

	/**
	 * Message with this code is sent to the result receiver fails and returns
	 * corresponding Http status code.
	 */
	public final static int MSG_STATUS_FAILURE = 0x00000013;

	/**
	 * Message with this code is sent to the result receiver when request fails
	 * due to connectivity loss or IO error that usually lead to exception to be
	 * thrown.
	 */
	public final static int MSG_EXCEPTION = 0x0FFFFFFF;

	/**
	 * Result listener interface.
	 */
	public static interface ResultListener {
		/**
		 * Called when result receiver receives results from the service.
		 * 
		 * @param code
		 *            Status code of the result.
		 * @param tag
		 *            Tag value.
		 * @param data
		 *            {@link android.os.Bundle} which contains command execution results.
		 */
		void onReceiveResult(int code, int tag, Bundle data);
	}

	private final static Handler mainThreadHandler = new Handler();
	private final static Handler receiverHandler;

	static {
		final HandlerThread thread = new HandlerThread(
				DeviceHiveResultReceiver.class.getSimpleName()
						+ "[Handler Thread]");
		thread.start();
		receiverHandler = new Handler(thread.getLooper());
	}

	private static class ResultListenerConfig {
		private final ResultListener listener;
		private final boolean runOnMainThread;

		public ResultListenerConfig(ResultListener listener,
				boolean runOnMainThread) {
			this.listener = listener;
			this.runOnMainThread = runOnMainThread;
		}
	}

	private ResultListenerConfig resultListenerConfig = null;

	public DeviceHiveResultReceiver() {
		super(receiverHandler);
	}

	/**
	 * Detach result listener from the result receiver.
	 */
	public void detachResultListener() {
		this.resultListenerConfig = null;
	}

	/**
	 * Set result listener.
	 * 
	 * @param listener
	 *            {@link com.dataart.android.devicehive.network.DeviceHiveResultReceiver.ResultListener} instance.
	 * @param runOnMainThread
	 *            Whether
	 *            {@link com.dataart.android.devicehive.network.DeviceHiveResultReceiver.ResultListener#onReceiveResult(int, int, android.os.Bundle)}
	 *            should run on the main Thread.
	 */
	public void setResultListener(final ResultListener listener,
			final boolean runOnMainThread) {
		this.resultListenerConfig = new ResultListenerConfig(listener,
				runOnMainThread);
	}

	@Override
	protected void onReceiveResult(final int resultCode, final Bundle resultData) {
		if (resultListenerConfig != null) {
			final int commandTagId = getIdForTag(NetworkCommand
					.getCommandTag(resultData));
			if (resultListenerConfig.runOnMainThread) {
				mainThreadHandler.post(new Runnable() {
					@Override
					public void run() {
						if (resultListenerConfig != null) {
							resultListenerConfig.listener.onReceiveResult(
									resultCode, commandTagId, resultData);
						}
					}
				});
			} else {
				resultListenerConfig.listener.onReceiveResult(resultCode,
						commandTagId, resultData);
			}
		} else {
			Log.w(DeviceHive.TAG, String.format(
					"Received result in detached listener: %s, %s", resultCode,
					resultData));
		}
	}

	/**
	 * Get tag id for given command tag. Returns the same value for equal tag
	 * strings.
	 * 
	 * @param tag
	 *            Command tag.
	 * @return Integer value corresponding to the tag.
	 */
	public static final int getIdForTag(final String tag) {
		Integer id = existingTags.get(tag);
		if (id == null) {
			id = tagIdCounter.incrementAndGet();
			existingTags.put(tag, id);
		}
		return id.intValue();
	}

	private final static AtomicInteger tagIdCounter = new AtomicInteger(0);
	private final static Map<String, Integer> existingTags = new HashMap<String, Integer>();
}
