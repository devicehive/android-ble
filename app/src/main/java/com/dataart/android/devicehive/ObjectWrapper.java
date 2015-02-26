package com.dataart.android.devicehive;

import java.io.Serializable;

public class ObjectWrapper<T extends Serializable> implements Serializable {

	private static final long serialVersionUID = -7373759268902337088L;

	private final T object;

	public ObjectWrapper(T object) {
		this.object = object;
	}

	public T getObject() {
		return object;
	}

	@Override
	public String toString() {
		if (object == null) {
			return "" + object;
		} else {
			return object.toString();
		}
	}

}
