package dev.buildcli.cli.commands.config;

import dev.buildcli.cli.BuildCLI;
import dev.buildcli.cli.utils.CommandUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

class AvailableCommandTest {
  @BeforeAll
  static void setUp() {
    //Disable banner
    CommandUtils.call("config", "set", "buildcli.logging.banner.enabled=false");
  }

  @Test
  void shouldListAllConfigs_whenRunAvailableCommand() {
    var cmd = new CommandLine(new BuildCLI());

    var sw = new StringWriter();
    cmd.setOut(new PrintWriter(sw));

    int exitCode = cmd.execute("config", "available");

    System.out.println(sw.toString());

    Assertions.assertEquals(0, exitCode);
    /*Assertions.assertTrue(sw.toString().contains("buildcli.logging.banner.enabled"));
    Assertions.assertTrue(sw.toString().contains("buildcli.logging.banner.path"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.vendor"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.model"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.url"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.token"));
    Assertions.assertTrue(sw.toString().contains("buildcli.plugins.paths"));*/
  }

  @Test
  void shouldListAllConfigs_whenRunACommand() {
    var cmd = new CommandLine(new BuildCLI());

    var sw = new StringWriter();
    cmd.setOut(new PrintWriter(sw));

    int exitCode = cmd.execute("config", "a");

    Assertions.assertEquals(0, exitCode);
    /*Assertions.assertTrue(sw.toString().contains("buildcli.logging.banner.enabled"));
    Assertions.assertTrue(sw.toString().contains("buildcli.logging.banner.path"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.vendor"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.model"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.url"));
    Assertions.assertTrue(sw.toString().contains("buildcli.ai.token"));
    Assertions.assertTrue(sw.toString().contains("buildcli.plugins.paths"));*/
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
