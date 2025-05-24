package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class AutoCompleteManagerTest {

    private ByteArrayOutputStream outputStream;

    private AutoCompleteManager manager;

    @Test
    public void testSetupAutocomplete() {

        outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        manager = new AutoCompleteManager();

        manager.setupAutocomplete();
        String output = outputStream.toString();

        assertTrue(output.contains("Detected shells") || output.contains("No supported shell detected"),
                "Expected output to contain 'Detected shells' or 'No supported shell detected'");

      if (output.contains("Detected shells")) {
            assertTrue(output.contains("bash") || output.contains("zsh") || output.contains("fish"),
                    "Expected output to contain the name of a detected shell (bash, zsh, fish).");
        }
    }

}
