package dev.buildcli.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.buildcli.core.utils.BeautifyShell.*;

/**
 * Enhanced logging utility that provides structured logging with color support.
 * This class bridges the gap between legacy System.out usage and proper SLF4J logging.
 */
public class SystemOutLogger {

	private static final Logger logger = LoggerFactory.getLogger("BuildCLI");

	private SystemOutLogger() { }

	/**
	 * Log an info message (maintains backward compatibility)
	 * @param message the message to log
	 */
	public static void log(String message) {
		logger.info(message);
	}

	/**
	 * Log an info message with blue color
	 * @param message the message to log
	 */
	public static void info(String message) {
		logger.info(blueFg(message));
	}

	/**
	 * Log a success message with green color
	 * @param message the message to log
	 */
	public static void success(String message) {
		logger.info(greenFg(message));
	}

	/**
	 * Log a warning message with yellow color
	 * @param message the message to log
	 */
	public static void warn(String message) {
		logger.warn(yellowFg(message));
	}

	/**
	 * Log an error message with red color
	 * @param message the message to log
	 */
	public static void error(String message) {
		logger.error(redFg(message));
	}

	/**
	 * Log an error message with exception
	 * @param message the message to log
	 * @param throwable the exception
	 */
	public static void error(String message, Throwable throwable) {
		logger.error(redFg(message), throwable);
	}

	/**
	 * Log a debug message (only visible when debug is enabled)
	 * @param message the message to log
	 */
	public static void debug(String message) {
		logger.debug(message);
	}

	/**
	 * Print a message to console without logging to file (for interactive output)
	 * This should be used sparingly, only for user interaction or progress indicators
	 * @param message the message to print
	 */
	public static void print(String message) {
		System.out.print(message);
	}

	/**
	 * Print a line to console without logging to file (for interactive output)
	 * This should be used sparingly, only for user interaction or progress indicators
	 * @param message the message to print
	 */
	public static void println(String message) {
		System.out.println(message);
	}

	/**
	 * Print an error to console without logging to file (for critical system errors)
	 * @param message the message to print
	 */
	public static void printError(String message) {
		System.err.println(redFg(message));
	}
}
