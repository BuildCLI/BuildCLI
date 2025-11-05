package dev.buildcli.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SystemOutLogger {

	private static final Logger logger = LoggerFactory.getLogger("BuildCLI");

	private SystemOutLogger() { }

	public static void log(String message) {
		logger.info(message);
	}
}
