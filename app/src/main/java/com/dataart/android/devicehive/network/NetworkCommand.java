package com.dataart.android.devicehive.network;

import java.io.IOException;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Base64;
import android.util.Log;

import com.dataart.android.devicehive.DeviceHive;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;

/**
 * Common base class for all service commands.
 */
public abstract class NetworkCommand implements Parcelable {
	
	protected final static ClassLoader CLASS_LOADER = NetworkCommand.class
			.getClassLoader();

	private final static String NAMESPACE = NetworkCommand.class.getName();

	private final static String KEY_TAG = NAMESPACE.concat(".KEY_TAG");
	
	private final static String KEY_STATUS_CODE = NAMESPACE
			.concat(".KEY_STATUS_CODE");

	private final static String KEY_EXCEPTION = NAMESPACE
			.concat(".KEY_EXCEPTION");

	private final static String KEY_COMMAND = NAMESPACE.concat(".KEY_COMMAND");

	private NetworkCommandConfig config;

	private static final HttpClient client = getClient();

	// timeouts
	private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 40000;
	private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 40000;

	/**
	 * Http request type.
	 */
	protected static enum RequestType {
		GET, POST, PUT, DELETE
	}
	
	/**
	 * Start this command using given {@link android.content.Context} and {@link android.os.ResultReceiver}
	 * .
	 * 
	 * @param context
	 *            {@link android.content.Context} instance which is used to start command.
	 * @param config
	 *            {@link NetworkCommandConfig} instance which contains configuration data for given command.
	 */
	public void start(final Context context, final NetworkCommandConfig config) {
		final Intent intent = new Intent(context, DeviceHiveApiService.class);
		intent.putExtra(DeviceHiveApiService.EXTRA_COMMAND, this);
		intent.putExtra(DeviceHiveApiService.EXTRA_COMMAND_CONFIG, config);
		intent.putExtra(DeviceHiveApiService.EXTRA_COMMAND_SERIAL, isSerial());
		context.startService(intent);
	}
	
	/* package */ void setConfig(NetworkCommandConfig config) {
		this.config = config;
	}

	/**
	 * Override this method in order to force commands to be executed in a
	 * "serial" mode, i.e. in order they submitted (in single executor). Default
	 * implementation returns <code>false</code>.
	 * 
	 * @return true, if command should be executed in "serial" mode, otherwise
	 *         return false.
	 */
	protected boolean isSerial() {
		return false;
	}

	/**
	 * This convenient method is used to ensure the bundle has correct
	 * command-tag.
	 * 
	 * @return Tagged {@link android.os.Bundle} instance.
	 */
	protected final Bundle getTaggedBundle() {
		return getTaggedBundle(0);
	}

	/**
	 * Create a {@link android.os.Bundle} instance and ensure the bundle has correct
	 * command-tag.
	 * 
	 * @param capacity
	 *            {@link android.os.Bundle} initial capacity.
	 * @return Tagged {@link android.os.Bundle} instance.
	 */
	protected final Bundle getTaggedBundle(final int capacity) {
		final Bundle bundle = capacity > 0 ? new Bundle(capacity + 1)
				: new Bundle();
		bundle.putString(KEY_TAG, getClass().getName());
		return bundle;
	}

	/**
	 * Get Http request type.
	 * 
	 * @returnm {@link com.dataart.android.devicehive.network.NetworkCommand.RequestType}
	 */
	protected abstract RequestType getRequestType();

	/**
	 * Get request path component.
	 * 
	 * @return Request path component.
	 */
	protected abstract String getRequestPath();

	/**
	 * Get request headers.
	 * 
	 * @return Request headers.
	 */
	protected abstract Map<String, String> getHeaders();

	/**
	 * Get {@link org.apache.http.HttpEntity} for the request. Override this method to return
	 * appropriate entity for POST and PUT requests.
	 * 
	 * @return {@link org.apache.http.HttpEntity} implementer instance.
	 */
	protected HttpEntity getRequestEntity() {
		return null;
	}

	/**
	 * Check if given status code is considered as successful for this request.
	 * Can be overridden in descendants.
	 * 
	 * @param statusCode
	 *            Response status code.
	 */
	protected boolean isSuccessStatusCode(int statusCode) {
		return statusCode == 200 || statusCode == 201;
	}

	/**
	 * Handle response.
	 * @param response String representation of the response body.
	 * @param resultData {@link android.os.Bundle} which is used to put and pass the results of command execution to the sender.
	 * @param context Current {@link android.content.Context} object.
	 * @return Status code.
	 * @see {@link DeviceHiveResultReceiver}.
	 */
	protected int handleResponse(final String response,
			final Bundle resultData, final Context context) {
		return -1;
	}

	/**
	 * Execute command. Called by the service on the dedicated thread.
	 * @param context Current {@link android.content.Context} object.
	 */
	protected void execute(Context context) {
		logD("Request started");
		final long start = System.currentTimeMillis();
		final Bundle resultData = getTaggedBundle();
		resultData.putParcelable(KEY_COMMAND, this);
		send(DeviceHiveResultReceiver.MSG_EXECUTE_REQUEST, resultData);

		HttpUriRequest httpRequest = toHttpRequest();
		addHeaders(httpRequest);
		final UsernamePasswordCredentials creds = config.getBasicAuthorisation();
		if (creds != null) {
		//	addBasicAuthenticationHeader(httpRequest, creds);
		}

		try {
			HttpResponse responce = client.execute(httpRequest);
			final int statusCode = responce.getStatusLine().getStatusCode();
            HttpEntity entity = responce.getEntity();

            String responceString = "";
            if(entity != null) {
                responceString = EntityUtils.toString(entity);
            } else {
                responceString = "";
            }


			logD("Responce status code: " + statusCode);
			logD("Responce body: " + responceString);
			resultData.putInt(KEY_STATUS_CODE, statusCode);
			if (!isSuccessStatusCode(statusCode)) {
				logD("Responce code is not OK. Responce body: "
						+ responceString);
				send(DeviceHiveResultReceiver.MSG_STATUS_FAILURE, resultData);
			} else {
				final int resultCode = handleResponse(responceString,
						resultData, context);
				if (resultCode > 0) {
					send(resultCode, resultData);
				}
			}
		} catch (ClientProtocolException e) {
			Log.e(DeviceHive.TAG, "Failed to execute request: ", e);
			resultData.putSerializable(KEY_EXCEPTION, e);
			send(DeviceHiveResultReceiver.MSG_EXCEPTION, resultData);
		} catch (IOException e) {
			Log.e(DeviceHive.TAG, "Failed to execute request: ", e);
			resultData.putSerializable(KEY_EXCEPTION, e);
			send(DeviceHiveResultReceiver.MSG_EXCEPTION, resultData);
		} finally {
			send(DeviceHiveResultReceiver.MSG_COMPLETE_REQUEST, resultData);
			long end = System.currentTimeMillis();
			logD(String.format("Request to %s completed: ", getTargetUrl())
					+ (end - start) + "ms");
		}
	}

	private HttpUriRequest addBasicAuthenticationHeader(HttpUriRequest request,
			UsernamePasswordCredentials creds) {
		Header hdr = BasicScheme.authenticate(creds, "utf-8", false);
		request.addHeader(hdr);
		return request;
	}

	private void send(final int resultCode, Bundle resultData) {
		if (config != null && config.resultReceiver != null) {
			if (resultData == null) {
				resultData = getTaggedBundle();
			} else if (!resultData.containsKey(KEY_TAG)) {
				resultData.putString(KEY_TAG, getClass().getName());
			}
			config.resultReceiver.send(resultCode, resultData);
		}
	}

	private String getTargetUrl() {
        String url = config.baseUrl + "/" + getRequestPath();
		return url;
	}

	private void addHeaders(HttpUriRequest request) {
		Map<String, String> headers = getHeaders();
		for (Map.Entry<String, String> header : headers.entrySet()) {
			request.addHeader(header.getKey(), header.getValue());
		}


//        request.addHeader( //"Basic c3U6YXNkQVNEcXdlMTIz");
//                "Authorization", "Basic " +
//                        Base64.encode("myuser:mypass".getBytes(), Base64.DEFAULT));


        //"Basic c3U6YXNkQVNEcXdlMTIz");

        BTLEDevicePreferences pref = new BTLEDevicePreferences();
        String s = pref.getUsername() + ":" + pref.getPassword();
        s = "Basic " + Base64.encodeToString(s.getBytes(), Base64.NO_WRAP);

        request.addHeader("Authorization",s);
	}

	private HttpUriRequest toHttpRequest() {
		String requestUrl = getTargetUrl();
		switch (getRequestType()) {
		case POST: {
			HttpPost request = new HttpPost(requestUrl);
			HttpEntity entity = getRequestEntity();
			if (entity != null) {
				request.setEntity(entity);
			}
			return request;
		}
		case GET: {
			return new HttpGet(requestUrl);
		}
		case PUT: {
			HttpPut request = new HttpPut(requestUrl);
			HttpEntity entity = getRequestEntity();
			if (entity != null) {
				request.setEntity(entity);
			}
			return request;
		}
		case DELETE: {
			return new HttpDelete(requestUrl);
		}
		default:
			logD("Unrecognized request type: " + getRequestType());
			return null;
		}
	}

	private static DefaultHttpClient getClient() {
		HttpParams params = getDefaultHttpParams();
		SchemeRegistry registry = getDefaultSchemeRegistry();

		ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(
				params, registry);

		return new DefaultHttpClient(manager, params);
	}

	private static SchemeRegistry getDefaultSchemeRegistry() {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		final SSLSocketFactory sslSocketFactory = SSLSocketFactory
				.getSocketFactory();
		sslSocketFactory
				.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		registry.register(new Scheme("https", sslSocketFactory, 443));
		return registry;
	}

	private static HttpParams getDefaultHttpParams() {
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf-8");
		params.setBooleanParameter("http.protocol.expect-continue", false);
		HttpConnectionParams.setConnectionTimeout(params,
				DEFAULT_CONNECTION_TIMEOUT_MILLIS);
		HttpConnectionParams
				.setSoTimeout(params, DEFAULT_SOCKET_TIMEOUT_MILLIS);
		return params;
	}
	
	private void logD(final String message) {
		if (config.isDebugLoggingEnabled) {
			Log.d(DeviceHive.TAG, message);
		}
	}

	/**
	 * Get command tag from tagged {@link android.os.Bundle}.
	 * @param resultData Tagged {@link android.os.Bundle}.
	 * @return Command tag.
	 */
	public final static String getCommandTag(final Bundle resultData) {
		return resultData.getString(KEY_TAG);
	}

	/**
	 * Get {@link Throwable} from tagged {@link android.os.Bundle}.
	 * @param resultData Tagged {@link android.os.Bundle}.
	 * @return {@link Throwable} if any exceptions occur during command execution, otherwise return null.
	 */
	public final static Throwable getThrowable(final Bundle resultData) {
		return (Throwable) resultData.getSerializable(KEY_EXCEPTION);
	}

	/**
	 * Get {@link NetworkCommand} from tagged {@link android.os.Bundle}.
	 * @param resultData Tagged {@link android.os.Bundle}.
	 * @return Current {@link NetworkCommand} instance.
	 */
	public final static NetworkCommand getCommand(final Bundle resultData) {
		return (NetworkCommand) resultData.getParcelable(KEY_COMMAND);
	}

	/**
	 * Get status code from tagged {@link android.os.Bundle}.
	 * @param resultData Tagged {@link android.os.Bundle}.
	 * @return Status code.
	 */
	public final static int getStatusCode(final Bundle resultData) {
		return resultData.getInt(KEY_STATUS_CODE);
	}

}
