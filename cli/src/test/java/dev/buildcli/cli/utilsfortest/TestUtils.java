package dev.buildcli.cli.utilsfortest;

import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringWriter;

public class TestUtils {

  public static CommandResult executeCommand(
      Class<?> cliClass,
      String... args
  ) {
    var cmd = new CommandLine(cliClass);
    var outSw = new StringWriter();
    var errSw = new StringWriter();
    var systemOut = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;

    cmd.setOut(new PrintWriter(outSw));
    cmd.setErr(new PrintWriter(errSw));
    try {
      System.setOut(new PrintStream(systemOut));
      int exitCode = cmd.execute(args);
      return new CommandResult(
          exitCode,
          outSw.toString() + systemOut.toString(),
          errSw.toString()
      );
    } finally {
      System.setOut(originalOut);
    }
  }

  public static class CommandResult {
    public final int exitCode;
    public final String output;
    public final String error;

    public CommandResult(int exitCode, String output, String error) {
      this.exitCode = exitCode;
      this.output = output;
      this.error = error;
    }
  }

}
