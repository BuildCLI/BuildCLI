package dev.buildcli.cli.commands.config;

import dev.buildcli.cli.CommandLineRunner;
import dev.buildcli.cli.BuildCLI;
import dev.buildcli.cli.utils.CommandUtils;
import dev.buildcli.cli.utilsfortest.ConfigKeys;
import dev.buildcli.cli.utilsfortest.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

class AvailableCommandTest {
  @BeforeAll
  static void setUp() {
    //Disable banner
    CommandUtils.call("config", "set", "buildcli.logging.banner.enabled=false");
  }

  @ParameterizedTest
  @ValueSource(strings = {"available", "a"})
  void shouldSuccess_whenRunValidCommands(String command) {
    var result = TestUtils.executeCommand(CommandLineRunner.class, "config", command);

    Assertions.assertEquals(0, result.exitCode);
    ConfigKeys.ALL_KEYS.forEach(
        key -> Assertions.assertTrue(result.output.contains(key), "Key not found: " + key));
  }

  @ParameterizedTest
  @ValueSource(strings = {"az"})
  void shouldFail_whenRunInvalidCommands(String command) {
    var result = TestUtils.executeCommand(CommandLineRunner.class, "config", command);

    Assertions.assertEquals(2, result.exitCode);
  }

  @Test
  void shouldError_whenRunWrongCommand() {
    var cmd = new CommandLine(new BuildCLI());

    var sw = new StringWriter();
    cmd.setOut(new PrintWriter(sw));

    int exitCode = cmd.execute("config", "az");

    Assertions.assertEquals(2, exitCode);
   /* Assertions.assertTrue(sw.toString().contains("Unmatched argument at index 1: 'az'"));
    Assertions.assertTrue(sw.toString().contains("Usage: buildcli config [-hV] [[-g] | [-l]] [COMMAND]"));*/
  }
}
