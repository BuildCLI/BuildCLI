package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class AutoCompleteManagerTest {

    @Test
    public void testSetupAutocomplete() {
        // Redirecionando a saída para capturar prints
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        AutoCompleteManager manager = new AutoCompleteManager();

        // Chamando o método de autocomplete
        manager.setupAutocomplete();

        String output = outputStream.toString();

        // Verificando se o resultado contém uma das mensagens esperadas
        assertTrue(output.contains("Detected shells") || output.contains("No supported shell detected"),
                "Expected output to contain 'Detected shells' or 'No supported shell detected'");

        // Verifica se pelo menos um dos shells esperados foi detectado
        if (output.contains("Detected shells")) {
            assertTrue(output.contains("bash") || output.contains("zsh") || output.contains("fish"),
                    "Expected output to contain the name of a detected shell (bash, zsh, fish).");
        }
    }

    @Test
    public void testSetupAutocompleteWithNoShellsDetected() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        AutoCompleteManager manager = new AutoCompleteManager();

        manager.setupAutocomplete();

        String output = outputStream.toString();

        assertTrue(output.contains("No supported shell detected"),
                "Expected output to contain 'No supported shell detected' when no shells are found.");
    }
}
