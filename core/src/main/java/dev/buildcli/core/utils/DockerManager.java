package dev.buildcli.core.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerManager {
    private static final Logger logger = Logger.getLogger(DockerManager.class.getName());

    public void setupDocker() {
        try {
            createDockerfile();
            System.out.println("Dockerfile created successfully.");
            System.out.println("Use 'buildcli --docker-build' to build and run the Docker container.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to setup Docker", e);
            System.err.println("Error: Could not setup Docker environment.");
        }
    }

    private void createDockerfile() throws IOException {
        File dockerfile = new File("Dockerfile");
        if (dockerfile.createNewFile()) {
            try (FileWriter writer = new FileWriter(dockerfile)) {
                writer.write("""
                        FROM openjdk:17-jdk-slim
                        WORKDIR /app
                        COPY target/*.jar app.jar
                        EXPOSE 8080
                        CMD ["java", "-jar", "app.jar"]
                        """);
                System.out.println("Dockerfile generated.");
            }
        } else {
            System.out.println("Dockerfile already exists.");
        }
    }
}
