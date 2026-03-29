package dev.buildcli.core.log;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class SystemOutLoggerTest {

    @ParameterizedTest
    @NullAndEmptySource // Tests for null and empty
    @ValueSource(strings = {
        " ",  // Tests for whitespace
        "valid message", // Tests for valid string
        "!@#$%^&*()", // Tests for special characters
        "formatted\ntext", //Tests for formatted text
        "{}" // Test for placeholder
    })

    void testSystemOutLogger_doesNotThrow(String input) {

        assertDoesNotThrow(() -> SystemOutLogger.log(input));
    }

    @Test
    void testSystemOutLogger_withLongInput() {
        // Tests for long input
        assertDoesNotThrow(() -> SystemOutLogger.log(
            "a".repeat(10000)));
    }

}
