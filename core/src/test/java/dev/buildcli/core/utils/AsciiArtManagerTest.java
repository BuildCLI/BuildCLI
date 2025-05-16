package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AsciiArtManagerTest {

    @Test
    void testEmptyArgs_shouldReturnFalse() {
        // When args is empty
        assertFalse(AsciiArtManager.shouldShowAsciiArt(new String[]{}));
    }

    @Test
    void testHelpFlag_shouldReturnTrue() {
        // When args contains --help
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"--help"}));
    }

    @ParameterizedTest
    @MethodSource("provideProjectCommands")
    void testProjectCommands(String[] args, boolean expected) {
        assertEquals(expected, AsciiArtManager.shouldShowAsciiArt(args));
    }

    private static Stream<Arguments> provideProjectCommands() {
        return Stream.of(
                Arguments.of(new String[]{"p", "run"}, true),
                Arguments.of(new String[]{"project", "run"}, true),
                Arguments.of(new String[]{"p", "i", "-n"}, true),
                Arguments.of(new String[]{"project", "i", "-n"}, true),
                Arguments.of(new String[]{"p", "build"}, false),
                Arguments.of(new String[]{"p", "i"}, false)
        );
    }

    @Test
    void testAboutCommand_shouldReturnTrue() {
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"about"}));
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"a"}));
    }

    @Test
    void testHelpCommand_shouldReturnTrue() {
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"help"}));
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"h"}));
    }

    @Test
    void testUnknownCommand_shouldReturnFalse() {
        assertFalse(AsciiArtManager.shouldShowAsciiArt(new String[]{"invalid"}));
    }
}