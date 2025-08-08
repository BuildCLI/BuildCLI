package dev.buildcli.core.constants;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigDefaultConstantsTest {

    @Test
    void testConfigFileName() {
        assertEquals("buildcli.properties", ConfigDefaultConstants.BUILD_CLI_CONFIG_FILE_NAME);
    }

    @Test
    void testGlobalConfigFilePath() {
        Path expectedPath = Path.of(System.getProperty("user.home"), ".buildcli", "buildcli.properties");
        assertEquals(expectedPath, ConfigDefaultConstants.BUILD_CLI_CONFIG_GLOBAL_FILE);
    }

    @Test
    void testComposePropertyNameWithNoParameter() {
        assertEquals("buildcli", ConfigDefaultConstants.composePropertyName());
    }

    @Test
    void testComposePropertyNameWithOneParameter() {
        assertEquals("buildcli.test1", ConfigDefaultConstants.composePropertyName("test1"));
    }

    @Test
    void testComposePropertyNameWithMultiParameters() {
        assertEquals("buildcli.test1.test2.test3", ConfigDefaultConstants.composePropertyName("test1", "test2", "test3"));
    }

}