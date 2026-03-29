package dev.buildcli.core.log;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

public class SystemOutLoggerTest {

    @Test
    void testSystemOutLogger_withValidInput() {
        // Test with valid input

        String input = "valid message";
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }

    @Test 
    void testSystemOutLogger_withEmptyString() {
        // Test with empty string

        String input = "";
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }

    @Test 
    void testSystemOutLogger_withWhitespace() {
        // Test with " "

        String input = " ";
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }
    
    @Test 
    void testSystemOutLogger_withNull() {
        // Test with null

        String input = null;
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }
    
    @Test 
    void testSystemOutLogger_withLongInput() {
        // Boundary test with long string

        String input = "a".repeat(10000);
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }

    @Test
    void testSystemOutLogger_withSpecialCharacters() {
        // Test with special characters 

        String input = "!@#$%^&*()";
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }

    @Test
    void testSystemOutLogger_withFormattedText() {
        // Test with formatted text
        String input = "formated\ntext";
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }

    @Test
    void testSystemOutLogger_withSLF4JPlaceholder() {
        // Test with SLF4J placeholder, to be treated as plaintext

        String input = "{}";
        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }

}
