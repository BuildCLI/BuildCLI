package dev.buildcli.cli.utilsfortest;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestUtils {

    public static CommandResult executeCommand (Class<?> cliClass, String... args){
        var cmd    = new CommandLine(cliClass);
        var outSw  = new StringWriter();
        var errSw  = new StringWriter();

        cmd.setOut(new PrintWriter(outSw));
        cmd.setErr(new PrintWriter(errSw));
        int exitCode = cmd.execute(args);
        return new CommandResult(exitCode, outSw.toString(), errSw.toString());
    }

    public static class CommandResult {
        public final int exitCode;
        public final String output;
        public final String error;

        public CommandResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output   = output;
            this.error    = error;
        }
    }

}
