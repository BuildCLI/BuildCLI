package dev.buildcli.core.utils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerBuildRunner {
    private static final Logger logger = Logger.getLogger(DockerBuildRunner.class.getName());

    public void buildAndRunDocker() {
        try {
            // Executar o comando "docker build"
            ProcessBuilder buildProcess = new ProcessBuilder(
                    "docker", "build", "-t", "buildcli-app", "."
            );
            buildProcess.inheritIO();
            int buildExitCode = buildProcess.start().waitFor();
            if (buildExitCode != 0) {
                logger.severe("Failed to build Docker image. Exit code: " + buildExitCode);
                return;
            }
            System.out.println("Docker image built successfully.");

            // Executar o comando "docker run"
            ProcessBuilder runProcess = new ProcessBuilder(
                    "docker", "run", "-p", "8080:8080", "buildcli-app"
            );
            runProcess.inheritIO();
            int runExitCode = runProcess.start().waitFor();
            if (runExitCode != 0) {
                logger.severe("Failed to run Docker container. Exit code: " + runExitCode);
            } else {
                System.out.println("Docker container is running on port 8080.");
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to build or run Docker container", e);
            Thread.currentThread().interrupt();
        }
    }
}
