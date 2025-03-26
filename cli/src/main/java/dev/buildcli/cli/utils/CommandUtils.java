package dev.buildcli.cli.utils;

import dev.buildcli.cli.BuildCLI;
import picocli.CommandLine;

public final class CommandUtils {
  public static int call(String... args) {
    return new CommandLine(new BuildCLI()).execute(args);
  }
}
