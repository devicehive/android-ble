package com.dataart.btle_android.devicehive;

import android.util.Log;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.EquipmentData;
import com.dataart.android.devicehive.device.CommandResult;
import com.dataart.android.devicehive.device.Equipment;
import com.dataart.android.devicehive.device.future.SimpleCallableFuture;

public class BTLEEquipment extends Equipment {

	private static final String TAG = "TestEquipment";

	public BTLEEquipment() {
		super(equipmentData());
	}

	private static EquipmentData equipmentData() {
		return new EquipmentData("Test Android Equipment",
				"Test Android Equipment code", "Test Android Equipment type");
	}
	
	@Override
	public void onBeforeRunCommand(Command command) {
		Log.d(TAG, "onBeforeRunCommand: " + command.getCommand());
	}

	@Override
	public boolean shouldRunCommandAsynchronously(final Command command) {
		return false;
	}

	@Override
	public SimpleCallableFuture<CommandResult> runCommand(final Command command) {
		Log.d(TAG, "runCommand: " + command.getCommand());
		
		// run command
		
		return new SimpleCallableFuture<CommandResult>(new CommandResult(CommandResult.STATUS_COMLETED,
				"Executed on Android test equipment!"));
	}

	@Override
	protected boolean onRegisterEquipment() {
		Log.d(TAG, "onRegisterEquipment");
		return true;
	}

	@Override
	protected boolean onUnregisterEquipment() {
		Log.d(TAG, "onUnregisterEquipment");
		return true;
	}

	@Override
	protected void onStartProcessingCommands() {
		Log.d(TAG, "onStartProcessingCommands");
	}

	@Override
	protected void onStopProcessingCommands() {
		Log.d(TAG, "onStopProcessingCommands");
	}

}
