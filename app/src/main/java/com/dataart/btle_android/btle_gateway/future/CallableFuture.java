package com.dataart.btle_android.btle_gateway.future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Constantine Mars on 4/1/15.
 *
 * Can be called and can return future result
 * It blocks on .get() and is notified from any other thread about result by calling .call()
 * But also can be used as simple wrapper for value in case of construction with argument or setting arg by .setArg() -
 * in that case it will not block on .get()
 */
public class CallableFuture<T, U> implements RunnableFuture<T> {

    private CallableWithArg<T, U> callable;
    private T result;
    private U arg;
    private boolean done;

    public CallableFuture(CallableWithArg<T, U> callable) { this.callable = callable; }

    public void call(U arg) {
        this.arg = arg;
        new Thread(this).start();
    }

//    Unlocks .get() immediately
    public void setArg(U arg) {
        this.arg = arg;
        result = this.callable.call(this.arg);
    }

    public CallableFuture(CallableWithArg<T, U> callable, U arg) {
        this.callable = callable;
        this.arg = arg;
        result = this.callable.call(this.arg);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (result == null) {
            synchronized (this) {
                wait();
            }
        }
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        if (result == null) {
            synchronized (this) {
                wait();
            }
        }
        return result;
    }

    @Override
    public void run() {
        done = false;
        try {
            result = callable.call(arg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        done = true;

        synchronized (this) { notifyAll(); }
    }

    public interface CallableWithArg<T, U> {
        T call(U u);
    }
}
