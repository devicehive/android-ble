package com.dataart.android.devicehive;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.dataart.android.devicehive.device.CommandResult;

/**
 * Represents a device command, a unit of information sent to devices.
 */
public class Command implements Parcelable {
	private int id;
	private String timestamp;
	private String command;
	private ObjectWrapper<Serializable> parameters;
	private int lifetime;
	private int flags;
	private String status;
	private String result;
	private UpdateCommandStatusCallback commandStatusCallback;

	public void setCommandStatusCallback(UpdateCommandStatusCallback commandStatusCallback) {
		this.commandStatusCallback = commandStatusCallback;
	}

//	called from place where command status updated
	public UpdateCommandStatusCallback getCommandStatusCallback() {
		return commandStatusCallback;
	}

	/* package */Command(int id, String timestamp, String command,
			Serializable parameters, int lifetime, int flags, String status,
			String result) {
		this.id = id;
		this.timestamp = timestamp;
		this.command = command;
		this.parameters = new ObjectWrapper<Serializable>(parameters);
		this.lifetime = lifetime;
		this.flags = flags;
		this.status = status;
		this.result = result;
	}

	public Command(String command, Serializable parameters, int lifetime,
			int flags) {
		this(-1, null, command, parameters, lifetime, flags, null, null);
	}

	/**
	 * Create command with given name and parameters.
	 * 
	 * @param command
	 *            Command name.
	 * @param parameters
	 *            Parameters dictionary.
	 */
	public Command(String command, Serializable parameters) {
		this(-1, null, command, parameters, 0, 0, null, null);
	}

	/**
	 * Get command identifier.
	 *
	 * @return Command identifier set by the server.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get command timestamp (UTC).
	 * 
	 * @return Datetime timestamp associated with this command.
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Get command name.
	 * 
	 * @return Command name.
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Get command parameters dictionary.
	 * 
	 * @return Command parameters dictionary.
	 */
	public Serializable getParameters() {
		return parameters != null ? parameters.getObject() : null;
	}

	/**
	 * Get command lifetime.
	 * 
	 * @return Number of seconds until this command expires.
	 */
	public int getLifetime() {
		return lifetime;
	}

	/**
	 * Get command flags. It's optional.
	 * 
	 * @return Value that could be supplied for device or related
	 *         infrastructure.
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * Get command status, as reported by device or related infrastructure.
	 * 
	 * @return Command status.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Get command execution result. It's optional value that could be provided
	 * by device.
	 * 
	 * @return Command execution result.
	 */
	public String getResult() {
		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(timestamp);
		dest.writeString(command);
		dest.writeSerializable(parameters != null ? parameters.getObject()
				: parameters);
		dest.writeInt(lifetime);
		dest.writeInt(flags);
		dest.writeString(status);
		dest.writeString(result);
	}

	public static Creator<Command> CREATOR = new Creator<Command>() {

		@Override
		public Command[] newArray(int size) {
			return new Command[size];
		}

		@Override
		public Command createFromParcel(Parcel source) {
			return new Command(source.readInt(), source.readString(),
					source.readString(), source.readSerializable(),
					source.readInt(), source.readInt(), source.readString(),
					source.readString());
		}
	};

	@Override
	public String toString() {
		return "Command [id=" + id + ", timestamp=" + timestamp + ", command="
				+ command + ", parameters=" + parameters + ", lifetime="
				+ lifetime + ", flags=" + flags + ", status=" + status
				+ ", result=" + result + "]";
	}

	abstract public static class UpdateCommandStatusCallback {
		private Object tag;

		public Object getTag() {
			return tag;
		}

		public void setTag(Object tag) {
			this.tag = tag;
		}

		abstract public void call(CommandResult result);
	}
}
