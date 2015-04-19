package com.dataart.android.devicehive.device.future;

/**
 * Created by Constantine Mars on 4/1/15.
 *
 * Simplified version of CallableFuture for use with single type for in and out parameters
 */
public class SimpleCallableFuture<T> extends CallableFuture<T, T> {
    public SimpleCallableFuture() {
        super(new SimpleCallableWithArg<T>());
    }

    public SimpleCallableFuture(T arg) {
        super(new SimpleCallableWithArg<T>(), arg);
    }

    public static class SimpleCallableWithArg<T> implements CallableWithArg<T, T> {
        @Override
        public T call(T arg) {
            return arg;
        }
    }
}
