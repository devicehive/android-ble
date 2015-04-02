package com.dataart.android.devicehive.device;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;

/**
 * Common interface for objects able to execute commands. Usually these are
 * {@link com.dataart.android.devicehive.device.Device} and {@link com.dataart.android.devicehive.device.Equipment}.
 */
public interface CommandRunner {

	/**
	 * Called right before command is executed either by the device itself or
	 * one of its equipment. This method is called for the device before each
	 * command execution (either by the device itself or one of its equipment).
	 * 
	 * @param command
	 *            {@link Command} to be executed.
	 */
	void onBeforeRunCommand(Command command);

	/**
	 * Check whether receiver of the command should execute command on some
	 * other thread, not on the main (UI) thread.
	 * 
	 * @param command
	 *            Command to be executed.
	 * @return true if {@link #runCommand(Command)} should be called
	 *         asynchronously, otherwise returns false.
	 */
	boolean shouldRunCommandAsynchronously(final Command command);

	/**
	 * Execute given command. Cab be called either on the main thread or some
	 * other thread. It depends on the value that is returned by
	 * {@link #shouldRunCommandAsynchronously(Command)} method.
	 * 
	 * @param command
	 *            Command to be executed.
	 * @return {@link com.dataart.android.devicehive.device.CommandResult} object describing command execution result
	 *         and status.
	 */
	SimpleCallableFuture<CommandResult> runCommand(final Command command);
}
