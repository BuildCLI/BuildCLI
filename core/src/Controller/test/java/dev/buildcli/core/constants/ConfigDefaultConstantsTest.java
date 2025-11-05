package dev.buildcli.core.constants;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for ConfigDefaultConstants class.
 */
class ConfigDefaultConstantsTest {

    @Test
    void testConfigFileConstants() {
        // Test file name constant
        assertEquals("buildcli.properties", ConfigDefaultConstants.BUILD_CLI_CONFIG_FILE_NAME);
        
        // Test global file path constant
        Path expectedPath = Path.of(System.getProperty("user.home"), ".buildcli", "buildcli.properties");
        assertEquals(ConfigDefaultConstants.BUILD_CLI_CONFIG_GLOBAL_FILE, expectedPath);
    }
    
    @Test
    void testLoggingConstants() {
        // Test logging parent constant
        assertEquals("logging", ConfigDefaultConstants.LOGGING_PARENT);
        
        // Test logging-related constants
        assertEquals("buildcli.logging.banner.enabled", ConfigDefaultConstants.BANNER_ENABLED);
        assertEquals("buildcli.logging.banner.path", ConfigDefaultConstants.BANNER_PATH);
        assertEquals("buildcli.logging.file.path", ConfigDefaultConstants.FILE_PATH);
        assertEquals("buildcli.logging.file.enabled", ConfigDefaultConstants.FILE_ENABLED);
    }
    
    @Test
    void testProjectConstants() {
        // Test project-related constants
        assertEquals("buildcli.project.name", ConfigDefaultConstants.PROJECT_NAME);
        assertEquals("buildcli.project.type", ConfigDefaultConstants.PROJECT_TYPE);
    }
    
    @Test
    void testAIConstants() {
        // Test AI parent constant
        assertEquals("ai", ConfigDefaultConstants.AI_PARENT);
        
        // Test AI-related constants
        assertEquals("buildcli.ai.vendor", ConfigDefaultConstants.AI_VENDOR);
        assertEquals("buildcli.ai.model", ConfigDefaultConstants.AI_MODEL);
        assertEquals("buildcli.ai.url", ConfigDefaultConstants.AI_URL);
        assertEquals("buildcli.ai.token", ConfigDefaultConstants.AI_TOKEN);
    }
    
    @Test
    void testPluginConstants() {
        // Test plugin parent constant
        assertEquals("plugin", ConfigDefaultConstants.PLUGIN_PARENT);
        
        // Test plugin-related constants
        assertEquals("buildcli.plugin.paths", ConfigDefaultConstants.PLUGIN_PATHS);
    }
    
    @Test
    void testComposePropertyName_SingleName() {
        // Test with a single name
        String result = ConfigDefaultConstants.composePropertyName("test");
        assertEquals("buildcli.test", result);
    }
    
    @Test
    void testComposePropertyName_MultipleNames() {
        // Test with multiple names
        String result = ConfigDefaultConstants.composePropertyName("parent", "child", "grandchild");
        assertEquals("buildcli.parent.child.grandchild", result);
    }
    
    @Test
    void testComposePropertyName_EmptyArray() {
        // Test with empty array
        String result = ConfigDefaultConstants.composePropertyName();
        assertEquals("buildcli", result);
    }
  
}

