package com.dataart.android.devicehive.network;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.dataart.android.devicehive.DeviceHive;
import com.dataart.android.devicehive.ObjectWrapper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Base command for JSON-related commands.
 * 
 */
public abstract class JsonNetworkCommand extends NetworkCommand {

	private final static Gson gson;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.setFieldNamingPolicy(FieldNamingPolicy.IDENTITY);
		builder.registerTypeAdapter(ObjectWrapper.class,
				new ObjectWrapperAdapter());
		gson = builder.create();
	}

	private static class ObjectWrapperAdapter implements
			JsonDeserializer<ObjectWrapper<Serializable>>,
			JsonSerializer<ObjectWrapper<Serializable>> {

		public JsonElement serialize(ObjectWrapper<Serializable> src,
				Type typeOfSrc, JsonSerializationContext context) {
			return context.serialize(src.getObject());
		}

		@Override
		public ObjectWrapper<Serializable> deserialize(JsonElement json,
				Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {

			return new ObjectWrapper<Serializable>(
					(Serializable) parseElement(json));
		}

		private Object parseElement(JsonElement jsonElement) {
			if (jsonElement.isJsonPrimitive()) {
				return parsePrimitive(jsonElement.getAsJsonPrimitive());
			} else if (jsonElement.isJsonArray()) {
				return parseArray(jsonElement.getAsJsonArray());
			} else {
				return parseObject(jsonElement.getAsJsonObject());
			}
		}

		private Object parsePrimitive(JsonPrimitive primitive) {
			if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			} else if (primitive.isNumber()) {
				return primitive.getAsDouble();
			} else {
				return primitive.getAsString();
			}
		}

		private ArrayList<Object> parseArray(JsonArray jsonArray) {
			final int size = jsonArray.size();
			ArrayList<Object> result = new ArrayList<Object>(size);
			for (int i = 0; i < size; i++) {
				result.add(parseElement(jsonArray.get(i)));
			}
			return result;
		}

		private Object parseObject(JsonObject jsonObject) {
			HashMap<String, Object> result = new HashMap<String, Object>();
			final Set<Entry<String, JsonElement>> entrySet = jsonObject
					.entrySet();
			for (Entry<String, JsonElement> property : entrySet) {
				result.put(property.getKey(), parseElement(property.getValue()));
			}
			return result;
		}
	}

	@Override
	protected Map<String, String> getHeaders() {
		final Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json; charset=utf-8");
		return headers;
	}

	@Override
	protected HttpEntity getRequestEntity() {
		String data = toJson(gson);
		HttpEntity entity = null;
		try {
			entity = new StringEntity(data, "utf-8");


		} catch (UnsupportedEncodingException e) {
			Log.e(DeviceHive.TAG, "Failed to create entity", e);
		}
		return entity;
	}

	protected abstract String toJson(final Gson gson);

	protected abstract int fromJson(final String response, final Gson gson,
			final Bundle resultData);

	@Override
	protected int handleResponse(final String response,
			final Bundle resultData, final Context context) {

		return fromJson(response, gson, resultData);
	}

	protected static String encodedString(String stringToEncode) {
		String encodedString = null;
		try {
			encodedString = URLEncoder.encode(stringToEncode, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("String to encode is illegal");
		}
		return encodedString;
	}

}
