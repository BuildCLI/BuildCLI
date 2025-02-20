package dev.buildcli.cli.log;

import dev.buildcli.cli.BuildCLI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SystemOutLogger {

	private static final Logger logger = LoggerFactory.getLogger(BuildCLI.class);

	private SystemOutLogger() { }

	public static void log(String message) {
		logger.info(message);
	}
}
