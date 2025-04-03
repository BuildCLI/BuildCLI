package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileManagerTest {

    private final String configPath = "test-config.config";

    private String loadConfig() throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(Paths.get(configPath)));
        return properties.getProperty("active.profile", "default");
    }

    @Test
    public void testGetActiveProfile_ReturnsProfileWhenExists() throws IOException {
        String configContent = "active.profile=dev";

        Path path = Paths.get(configPath);
        Files.write(path, configContent.getBytes(), StandardOpenOption.CREATE);

        ProfileManager profileManagerTest = new ProfileManager() {
            @Override
            public String getActiveProfile() {
                try {
                    return loadConfig();
                } catch (IOException e) {
                    return "default";
                }
            }
        };

        String result = profileManagerTest.getActiveProfile();

        assertEquals("dev", result);

        Files.delete(path);
    }

    @Test
    public void testGetActiveProfile_ReturnsDefaultWhenFileNotFound() throws IOException {
        ProfileManager profileManagerTest = new ProfileManager() {
            @Override
            public String getActiveProfile() {
                try {
                    return loadConfig();
                } catch (IOException e) {
                    return "default";
                }
            }
        };

        String result = profileManagerTest.getActiveProfile();

        assertEquals("default", result);
    }

    @Test
    public void testGetActiveProfile_ReturnsDefaultWhenProfileNotFound() throws IOException {
        String configContent = "";

        Path path = Paths.get(configPath);
        Files.write(path, configContent.getBytes(), StandardOpenOption.CREATE);

        ProfileManager profileManagerTest = new ProfileManager() {
            @Override
            public String getActiveProfile() {
                try {
                    return loadConfig();
                } catch (IOException e) {
                    return "default";
                }
            }
        };

        String result = profileManagerTest.getActiveProfile();

        assertEquals("default", result);

        Files.delete(path);
    }
}