package dev.buildcli.cli.commands.config;

import dev.buildcli.cli.BuildCLI;
import dev.buildcli.cli.utils.CommandUtils;
import dev.buildcli.cli.utilsfortest.ConfigKeys;
import dev.buildcli.cli.utilsfortest.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AvailableCommandTest {
  @BeforeAll
  static void setUp() {
    //Disable banner
    CommandUtils.call("config", "set", "buildcli.logging.banner.enabled=false");
  }

  @ParameterizedTest
  @ValueSource(strings = {"available", "a"})
  void shouldSuccess_whenRunValidCommands(String command) {
    var result = TestUtils.executeCommand(BuildCLI.class, "config", command);

    Assertions.assertEquals(0, result.exitCode);
    ConfigKeys.ALL_KEYS.forEach(
            key -> Assertions.assertTrue(result.output.contains(key), "Key not found: " + key));
  }

  @ParameterizedTest
  @ValueSource(strings = {"az"})
  void shouldFail_whenRunInvalidCommands(String command) {
    var result = TestUtils.executeCommand(BuildCLI.class, "config", command);

    Assertions.assertEquals(2, result.exitCode, "Exit code must be 2 for CLI errors");
    Assertions.assertTrue(result.error.contains("Unmatched argument at index 1: '" + command + "'"), "Missing error message");
    Assertions.assertTrue(result.error.contains("Usage: buildcli config [-hV] [[-g] | [-l]] [COMMAND]"), "Incorrect usage message");

  }
}
