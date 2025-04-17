package dev.buildcli.core.utils;

import dev.buildcli.core.domain.git.GitCommandExecutor;
import dev.buildcli.core.log.SystemOutLogger;

public class AboutService {
    private static GitCommandExecutor gitExec = new GitCommandExecutor();

    public AboutService(GitCommandExecutor gitCommandExecutor) {
        gitExec = gitCommandExecutor;
    }

    public static void about() {
        SystemOutLogger.log("BuildCLI is a command-line interface (CLI) tool for managing and automating common tasks in Java project development.\n" +
                "It allows you to create, compile, manage dependencies, and run Java projects directly from the terminal, simplifying the development process.\n");
        SystemOutLogger.log("Visit the repository for more details: https://github.com/BuildCLI/BuildCLI\n");

        SystemOutLogger.log(gitExec.showContributors());
    }
}
