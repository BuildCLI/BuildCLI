package dev.buildcli.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SemVerManager {

    private static final Logger logger = Logger.getLogger(SemVerManager.class.getName());
    private static final Path VERSION_FILE = Path.of("VERSION");

    public void manageVersion(String versionType) {
        try {
            String currentVersion = getCurrentVersion();
            String newVersion = incrementVersion(currentVersion, versionType);
            Files.writeString(VERSION_FILE, newVersion);

            System.out.printf("Version updated: %s -> %s%n", currentVersion, newVersion);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to manage versioning", e);
        }
    }

    private String getCurrentVersion() throws IOException {
        if (!Files.exists(VERSION_FILE)) {
            Files.writeString(VERSION_FILE, "0.0.0");
        }
        return Files.readString(VERSION_FILE).trim();
    }

    private String incrementVersion(String currentVersion, String versionType) {
        String[] parts = currentVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        switch (versionType.toLowerCase()) {
            case "major" -> { return (major + 1) + ".0.0"; }
            case "minor" -> { return major + "." + (minor + 1) + ".0"; }
            case "patch" -> { return major + "." + minor + "." + (patch + 1); }
            default -> throw new IllegalArgumentException("Invalid version type: " + versionType);
        }
    }
}
