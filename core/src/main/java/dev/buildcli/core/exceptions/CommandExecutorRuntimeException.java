package dev.buildcli.core.exceptions;

public class CommandExecutorRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CommandExecutorRuntimeException() {
	}

	public CommandExecutorRuntimeException(String message) {
		super(message);
	}

	public CommandExecutorRuntimeException(Throwable cause) {
		super(cause);
	}

	public CommandExecutorRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandExecutorRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
