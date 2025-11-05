package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsciiArtManagerTest {

    @Test
    void testShouldShowAsciiArt() {
        assertFalse(AsciiArtManager.shouldShowAsciiArt(new String[]{}));
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"--help"}));
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"p", "run"}));
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"p", "i", "-n"}));
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"about"}));
        assertTrue(AsciiArtManager.shouldShowAsciiArt(new String[]{"help"}));
        assertFalse(AsciiArtManager.shouldShowAsciiArt(new String[]{"invalid"}));
    }
}