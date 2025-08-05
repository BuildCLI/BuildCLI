package dev.buildcli.cli.commands.run;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.log.SystemOutLogger;
import picocli.CommandLine.Command;

import java.io.IOException;

@Command(name = "dockerfile", description = "Build and run Docker container", mixinStandardHelpOptions = true)
public class DockerfileCommand implements BuildCLICommand {

    @Override
    public void run() {
        try {
            // Executar o comando "docker build"
            ProcessBuilder buildProcess = new ProcessBuilder(
                    "docker", "build", "-t", "buildcli-app", "."
            );
            buildProcess.inheritIO();
            int buildExitCode = buildProcess.start().waitFor();
            if (buildExitCode != 0) {
                throw new RuntimeException("Failed to build Docker image. Exit code: " + buildExitCode);
            }
            SystemOutLogger.success("Docker image built successfully.");

            // Executar o comando "docker run"
            ProcessBuilder runProcess = new ProcessBuilder(
                    "docker", "run", "-p", "8080:8080", "buildcli-app"
            );
            runProcess.inheritIO();
            int runExitCode = runProcess.start().waitFor();
            if (runExitCode != 0) {
                throw new RuntimeException("Failed to run Docker container. Exit code: " + runExitCode);
            } else {
                SystemOutLogger.success("Docker container is running on port 8080.");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to build or run Docker container", e);
        }
    }
}
