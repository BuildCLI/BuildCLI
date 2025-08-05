package dev.buildcli.core.utils;


import dev.buildcli.core.actions.commandline.DockerProcess;


import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerBuildRunner {
    private static final Logger logger = Logger.getLogger(DockerBuildRunner.class.getName());
    private static final String IMAGE_TAG = "buildcli-app";

    public DockerBuildRunner() {
        // empty constructor for use in tests
    }

    public void buildAndRunDocker() {
        var buildProcess = DockerProcess.createBuildProcess(IMAGE_TAG);
        int buildExitCode = buildProcess.run();

        if (buildExitCode != 0) {
            logger.log(Level.INFO, "Failed with exit code: {}", buildExitCode);
            return;
        }
        System.out.println("Docker image built successfully.");

        var runProcess = DockerProcess.createRunProcess(IMAGE_TAG);
        int runExitCode = runProcess.run();

        if (runExitCode != 0) {
            logger.log(Level.INFO, "Failed to run Docker container. Exit code: {}", runExitCode);
        } else {
            System.out.println("Docker container is running on port 8080.");
        }
    }
}
