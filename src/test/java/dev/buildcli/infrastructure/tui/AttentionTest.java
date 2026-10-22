package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** When the window title changes and when the bell rings. */
class AttentionTest {
    final Attention a = new Attention();

    @Test
    void aNewQuestionRingsOnceAndNamesTheAgentInTheTitle() {
        var idle = a.update(0, 0, null, false, true);
        assertEquals("BuildCLI", idle.title());
        assertFalse(idle.bell());

        var ask = a.update(1_000, 1, "ana", true, true);
        assertEquals("● ana needs you · BuildCLI", ask.title());
        assertTrue(ask.bell());

        var still = a.update(1_080, 1, "ana", true, true);
        assertNull(still.title(), "nothing changed, so nothing is written");
        assertFalse(still.bell(), "it does not ring again while the same question waits");

        var second = a.update(2_000, 2, "ana", true, true);
        assertTrue(second.bell(), "a second question rings again");
    }

    @Test
    void aLongJobRingsWhenItEndsAndAShortOneDoesNot() {
        a.update(0, 0, null, true, true);
        var done = a.update(20_000, 0, null, false, true);
        assertTrue(done.bell());
        assertEquals("BuildCLI", done.title());

        a.update(30_000, 0, null, true, true);
        assertFalse(a.update(35_000, 0, null, false, true).bell(), "5 seconds: you were watching it");
    }

    @Test
    void aJobThatEndsWithAQuestionStillWaitingDoesNotRingTwice() {
        a.update(0, 0, null, true, true);
        var ask = a.update(20_000, 1, "bruno", true, true);
        assertTrue(ask.bell());
        var after = a.update(21_000, 1, "bruno", false, true);
        assertFalse(after.bell(), "the question already rang, and the job is not really over");
    }

    @Test
    void theSettingTurnsTheSoundOffButNotTheTitle() {
        var ask = a.update(0, 1, "ana", true, false);
        assertFalse(ask.bell());
        assertTrue(ask.title().contains("ana needs you"));
        a.update(1, 0, null, true, false);
        assertFalse(a.update(60_000, 0, null, false, false).bell());
    }

    @Test
    void theTitleShowsWhileTheTeamWorks() {
        assertEquals("BuildCLI · working", a.update(0, 0, null, true, true).title());
        assertEquals("BuildCLI", a.update(1_000, 0, null, false, true).title());
    }
}
