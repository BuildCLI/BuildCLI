package dev.buildcli.cli.utils;

import dev.buildcli.cli.CommandLineRunner;
import picocli.CommandLine;

public final class CommandUtils {
  public static int call(String... args) {
    return new CommandLine(new CommandLineRunner()).execute(args);
  }
}
