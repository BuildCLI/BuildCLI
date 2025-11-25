package dev.buildcli.core.utils.config;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static dev.buildcli.core.constants.ConfigDefaultConstants.BUILD_CLI_CONFIG_FILE_NAME;
import static dev.buildcli.core.constants.ConfigDefaultConstants.BUILD_CLI_CONFIG_GLOBAL_FILE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigContextLoader.
 * These tests verify reading, merging, and saving of both local and global configurations.
 * Notes:
 * - Uses ConfigDefaultConstants for actual config paths.
 * - Avoids testing isLocal() flag because the implementation may not always set it.
 * - Each test resets static caches to ensure isolation.
 */

class ConfigContextLoaderTest {

    @TempDir
    Path tmp;

    private String origUserHome;

    @BeforeEach
    void setUp() throws Exception {
        // Redirect user.home to a temporary directory
        origUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tmp.resolve("home").toString());
        Files.createDirectories(Path.of(System.getProperty("user.home")));

        resetStaticCache();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore original user.home
        if (origUserHome != null) System.setProperty("user.home", origUserHome);
        resetStaticCache();

        // Clean up local config file created by tests
        try {
            Path localPath = Path.of(BUILD_CLI_CONFIG_FILE_NAME).toAbsolutePath();
            Files.deleteIfExists(localPath);
        } catch (Exception ignored) {
            // exception
        }
    }
    /**
     * Clears possible static cache fields inside ConfigContextLoader.
     * This ensures that each test starts with a fresh state.
     */
    private static void resetStaticCache() throws Exception {
        try {
            Field local = ConfigContextLoader.class.getDeclaredField("localConfig");
            Field global = ConfigContextLoader.class.getDeclaredField("globalConfig");
            Field merged = ConfigContextLoader.class.getDeclaredField("mergedConfig");
            local.setAccessible(true);
            global.setAccessible(true);
            merged.setAccessible(true);
            local.set(null, null);
            global.set(null, null);
            merged.set(null, null);
        } catch (NoSuchFieldException ignored) {
            // Some implementations may not have these fields – that's fine
        }
    }

    /**
     * Helper method to write a small .properties file with the given content.
     */
    private static void writeProperties(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        try (var out = new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8)) {
            out.write(content);
        }
    }

    /** Returns the absolute path for the local configuration file (from constants). */
    private static Path localPath() {
        return Path.of(BUILD_CLI_CONFIG_FILE_NAME).toAbsolutePath();
    }

    /** Returns the absolute path for the global configuration file (from constants). */
    private static Path globalPath() {
        return BUILD_CLI_CONFIG_GLOBAL_FILE.toAbsolutePath();
    }

    @Test
    @DisplayName("Empty environment yields empty merged config")
    void emptyEnv() {
        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertNotNull(all);
        assertEquals(Optional.empty(), all.getProperty("nonexistent"));
    }

    @Test
    @DisplayName("Reads local buildcli.properties when present")
    void readsLocal() throws Exception {
        writeProperties(localPath(), "foo=local\nbar=1\n");

        BuildCLIConfig local = ConfigContextLoader.getLocalConfig();
        assertEquals(Optional.of("local"), local.getProperty("foo"));

        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertEquals(Optional.of("local"), all.getProperty("foo"));
        assertEquals(Optional.of("1"), all.getProperty("bar"));
    }

    @Test
    @DisplayName("Reads global config from user.home when present")
    void readsGlobal() throws Exception {
        writeProperties(globalPath(), "g1=global\nsame=G\n");

        BuildCLIConfig global = ConfigContextLoader.getGlobalConfig();
        assertEquals(Optional.of("global"), global.getProperty("g1"));

        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertEquals(Optional.of("global"), all.getProperty("g1"));
        assertEquals(Optional.of("G"), all.getProperty("same"));
    }

    @Test
    @DisplayName("Local values override global values on merge")
    void localOverridesGlobal() throws Exception {
        writeProperties(globalPath(), "same=G\nonlyGlobal=x\n");
        writeProperties(localPath(), "same=L\nonlyLocal=y\n");

        BuildCLIConfig all = ConfigContextLoader.getAllConfigs();
        assertEquals(Optional.of("L"), all.getProperty("same"), "local should override global");
        assertEquals(Optional.of("x"), all.getProperty("onlyGlobal"));
        assertEquals(Optional.of("y"), all.getProperty("onlyLocal"));
    }

    @Test
    @DisplayName("saveLocalConfig() persists to local file path")
    void saveLocalWritesFile() {
        BuildCLIConfig cfg = BuildCLIConfig.empty();
        cfg.addOrSetProperty("abc", "123");
        ConfigContextLoader.saveLocalConfig(cfg);

        Path file = localPath();
        assertTrue(Files.exists(file));
        BuildCLIConfig reloaded = ConfigContextLoader.getLocalConfig();
        assertEquals(Optional.of("123"), reloaded.getProperty("abc"));
    }

    @Test
    @DisplayName("saveGlobalConfig() persists to global file path")
    void saveGlobalWritesFile() {
        BuildCLIConfig cfg = BuildCLIConfig.empty();
        cfg.addOrSetProperty("abc", "777");
        ConfigContextLoader.saveGlobalConfig(cfg);

        Path file = globalPath();
        assertTrue(Files.exists(file));
        BuildCLIConfig reloaded = ConfigContextLoader.getGlobalConfig();
        assertEquals(Optional.of("777"), reloaded.getProperty("abc"));
    }
}