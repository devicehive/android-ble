package com.dataart.android.devicehive.network;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.dataart.android.devicehive.DeviceHive;

/**
 * Android {@link android.app.Service} subclass which is used to concurrently execute
 * commands sent via {@link android.content.Intent}. This service should be declared in
 * AndroidManifest.xml file. Can execute commands either concurrently or in FIFO
 * order.
 * 
 * @see {@link com.dataart.android.devicehive.network.NetworkCommand#isSerial()}
 */
public class DeviceHiveApiService extends Service {

	private final static String NAMESPACE = DeviceHiveApiService.class
			.getName();

	/* package */final static String EXTRA_COMMAND = NAMESPACE
			.concat(".EXTRA_COMMAND");

	/* package */final static String EXTRA_COMMAND_CONFIG = NAMESPACE
			.concat(".EXTRA_COMMAND_CONFIG");

	/* package */final static String EXTRA_COMMAND_SERIAL = NAMESPACE
			.concat(".EXTRA_COMMAND_SERIAL");

	private final static ThreadFactory threadFactory = new ThreadFactory() {

		private final AtomicInteger threadSerialNumber = new AtomicInteger(0);

		@Override
		public Thread newThread(Runnable r) {
			final Thread thread = new Thread(r, "[DeviceHiveApiService #"
					+ threadSerialNumber.getAndIncrement() + "]");
			thread.setDaemon(true);
			return thread;
		}
	};

	private final ConcurrentLinkedQueue<Integer> commandStartIdQueue = new ConcurrentLinkedQueue<Integer>();

	private final static ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
			4, 6, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10),
			threadFactory);

	private final static SerialExecutor SERIAL_EXECUTOR = new SerialExecutor();

	private static class SerialExecutor implements Executor {
		private final LinkedList<Runnable> tasks = new LinkedList<Runnable>();
		private Runnable activeTask;

		public synchronized void execute(final Runnable r) {
			tasks.offer(new Runnable() {
				public void run() {
					try {
						r.run();
					} finally {
						scheduleNext();
					}
				}
			});
			if (activeTask == null) {
				scheduleNext();
			}
		}

		protected synchronized void scheduleNext() {
			activeTask = tasks.poll();
			if (activeTask != null) {
				THREAD_POOL_EXECUTOR.execute(activeTask);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		commandStartIdQueue.add(startId);
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					handleIntent(intent);
				} finally {
					stopSelf(commandStartIdQueue.remove());
				}
			}
		};
		if (intent.getBooleanExtra(EXTRA_COMMAND_SERIAL, false)) {
			SERIAL_EXECUTOR.execute(runnable);
		} else {
			THREAD_POOL_EXECUTOR.execute(runnable);
		}
		return START_NOT_STICKY;
	}

	protected void handleIntent(Intent intent) {
		NetworkCommand command = null;
		NetworkCommandConfig config = null;
		final long startTime = System.currentTimeMillis();
		try {
			command = intent.getParcelableExtra(EXTRA_COMMAND);
			if (command != null) {
				config = intent.getParcelableExtra(EXTRA_COMMAND_CONFIG);
				if (config != null) {
					if (config.isDebugLoggingEnabled) {
						Log.d(DeviceHive.TAG, "Starting command " + command);
					}
					command.setConfig(config);
					command.execute(this);
				} else {
					Log.w(DeviceHive.TAG, "Missing command config in " + intent);
				}
			} else {
				Log.w(DeviceHive.TAG, "Missing command in " + intent);
			}
		} catch (Exception e) {
			Log.e(DeviceHive.TAG, "Cannot process command " + command, e);
		} finally {
			if (command != null && config != null) {
				if (config.isDebugLoggingEnabled) {
					Log.d(DeviceHive.TAG, "Completed command " + command
							+ " in " + (System.currentTimeMillis() - startTime));
				}
			}
		}
	}

}
