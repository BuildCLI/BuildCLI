package dev.buildcli.cli.service;

import dev.buildcli.core.log.SystemOutLogger;
import picocli.CommandLine;

public class WelcomeService {

    public void displayWelcomeBanner(SystemOutLogger logger, CommandLine commandLine) {
        logger.log(commandLine.getColorScheme().ansi().string("@|bold,magenta     _              _ _ _      _      _     \n" +
                "  __ | | ___ _ __ (_|_) | ___| |__  | |    \n" +
                " / _` |/ _ \ '_ \| | | |/ __| '_ \ | |    \n" +
                "| (_| |  __/ | | | | | | (__| | | |_| | \n" +
                " \__,_|\___|_| |_|_|_|_|\___|_| |_|(_)_|   \n" +
                "                                          |@"));
        logger.log("");
    }
}

