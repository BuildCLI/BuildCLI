package dev.buildcli.core.utils;

import java.io.File;
import java.io.IOException;

public class DirectoryService {

    /**
     * Get the build directory for a project
     * @param projectRoot The root directory of the project
     * @return The build directory
     */
    private File getBuildDirectory(File projectRoot) {
        // Check if the project is a Maven project
        File mavenTarget = new File(projectRoot, "target");
        if (mavenTarget.exists() && mavenTarget.isDirectory()) {
            return mavenTarget;
        }
        // Check if the project is a Gradle project
        File gradleTarget = new File(new File(projectRoot, "build"), "libs");
        if (gradleTarget.exists() && gradleTarget.isDirectory()) {
            return gradleTarget;
        }
        // Default to Maven's target directory
        return mavenTarget;
    }

    /**
     * Find the JAR file for a project
     * @param projectRoot The root directory of the project
     * @return The JAR file
     * @throws IOException If the build directory does not exist or is not a directory
     */
    public File findJar(File projectRoot) throws IOException {
        File buildDir = getBuildDirectory(projectRoot);
        // Check if the build directory exists and is a directory
        if (!buildDir.exists() || !buildDir.isDirectory()) {
            throw new IOException("Build directory does not exist or is not a directory: " + buildDir.getAbsolutePath());
        }

        // Check if the build directory contains a JAR file
        File[] jarFiles = buildDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            throw new IOException("No JAR file found in build directory: " + buildDir.getAbsolutePath());
        }

        // Assume that the first JAR file found is the correct one.
        return jarFiles[0];
    }
} 