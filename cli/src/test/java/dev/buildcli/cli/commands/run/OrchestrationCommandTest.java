package dev.buildcli.cli.commands.run;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrchestrationCommand Tests")
class OrchestrationCommandTest {

    @Test
    @DisplayName("Test that the run() method displays the correct usage message.")
    void testRunDisplaysUsage() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        try {
            System.setOut(new PrintStream(outContent));
            System.setErr(new PrintStream(errContent));

            CommandLine cmd = new CommandLine(new OrchestrationCommand());
            int exitCode = cmd.execute(new String[]{});

            String output = outContent.toString() + errContent.toString();

            assertAll(
                    () -> assertEquals(2, exitCode, "Should return error exit code"),
                    () -> assertTrue(output.contains("Usage: orchestration"), "Should show command name"),
                    () -> assertTrue(output.contains("Missing required subcommand"), "Should show error message"),
                    () -> assertTrue(output.contains("Manage container orchestration using Docker Compose."), "Should show description")
            );

            assertAll(
                    () -> assertTrue(output.contains("-h, --help"), "Should show help option"),
                    () -> assertTrue(output.contains("-V, --version"), "Should show version option")
            );

            assertAll(
                    () -> assertTrue(output.contains("Commands:"), "Should show commands section"),
                    () -> assertTrue(output.contains("up"), "Should show up command"),
                    () -> assertTrue(output.contains("down"), "Should show down command")
            );
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    @DisplayName("Test that the subcommands are correctly registered.")
    void testSubcommandsRegistered() {
        CommandLine cmd = new CommandLine(new OrchestrationCommand());

        assertTrue(cmd.getSubcommands().containsKey("up"));
        assertTrue(cmd.getSubcommands().containsKey("down"));
    }
}