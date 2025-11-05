package dev.buildcli.core.exceptions;

public class ExtractionRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExtractionRuntimeException() {
		super();
	}

	public ExtractionRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ExtractionRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExtractionRuntimeException(String message) {
		super(message);
	}

	public ExtractionRuntimeException(Throwable cause) {
		super(cause);
	}

	
}
