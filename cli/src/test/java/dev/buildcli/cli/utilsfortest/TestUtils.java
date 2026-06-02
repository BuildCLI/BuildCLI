package dev.buildcli.cli.utilsfortest;

import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringWriter;

public class TestUtils {

  /**
   * Executes a picocli command and captures its output streams.
   *
   * @param cliClass the command class
   * @param args the command arguments
   * @return the command result
   */
  public static CommandResult executeCommand(
      final Class<?> cliClass,
      final String... args
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

    /**
     * Creates a command result.
     *
     * @param exitCode the command exit code
     * @param output the captured standard output
     * @param error the captured error output
     */
    public CommandResult(
        final int exitCode,
        final String output,
        final String error
    ) {
      this.exitCode = exitCode;
      this.output = output;
      this.error = error;
    }
  }

}
