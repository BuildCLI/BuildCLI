package dev.buildcli.core.project;

import dev.buildcli.core.utils.SystemCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Deprecated(forRemoval = true)
public class ProjectCompiler {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCompiler.class);

    public void compileProject() {
        try {
            // Altere o comando para "package" em vez de "compile"
            ProcessBuilder builder = new ProcessBuilder(SystemCommands.MVN.getCommand(), "package");
            builder.inheritIO();
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Project compiled successfully. JAR file generated in target directory.");
            } else {
                logger.error("Failed to compile project. Maven exited with code: {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to compile project", e);
            Thread.currentThread().interrupt();
        }
    }
}
