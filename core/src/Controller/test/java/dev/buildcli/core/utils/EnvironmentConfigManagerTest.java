package dev.buildcli.core.utils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentConfigManagerTest {

    @TempDir
    Path tempDir;
    Path configFile;

    @BeforeEach
    void setUp() {
        configFile = tempDir.resolve("environment.config");
        EnvironmentConfigManager.setConfigPathForTest(configFile);
    }

    @AfterEach
    void tearDown() {
        EnvironmentConfigManager.setConfigPathForTest(Path.of("environment.config"));
    }

    @Test
    void testSetEnvironmentCreatesFileAndWritesCorrectly() throws IOException {
        String env = "dev";
        EnvironmentConfigManager.setEnvironment(env);

        assertTrue(Files.exists(configFile), "O arquivo de configuração deve ser criado.");
        String content = Files.readString(configFile);
        assertEquals("active.profile=dev", content);
    }

    @Test
    void testGetEnvironmentReturnsCorrectValue() {
        EnvironmentConfigManager.setEnvironment("prod");
        String result = EnvironmentConfigManager.getEnvironment();
        assertEquals("prod", result);
    }

    @Test
    void testGetEnvironmentReturnsNullIfFileDoesNotExist() {
        assertNull(EnvironmentConfigManager.getEnvironment());
    }

    @Test
    void testGetEnvironmentReturnsNullIfFormatIsInvalid() throws IOException {
        Files.writeString(configFile, "profile:dev");
        assertNull(EnvironmentConfigManager.getEnvironment());
    }

    @Test
    void testSetEnvironmentOverwritesExistingFile() throws IOException {
        Files.writeString(configFile, "active.profile=old");
        EnvironmentConfigManager.setEnvironment("new");
        String content = Files.readString(configFile);
        assertEquals("active.profile=new", content);
    }
}
