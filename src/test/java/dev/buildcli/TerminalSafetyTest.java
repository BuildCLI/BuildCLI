package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.tools.CommandGuard;
import dev.buildcli.infrastructure.SafePrintStream;
import dev.buildcli.infrastructure.TerminalText;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalSafetyTest {

    @Test
    void escapeSequencesAreMadeVisibleInsteadOfBeingInterpreted() {
        String out = TerminalText.sanitize("ok \u001b[2J\u001b]0;pwned\u0007 done");
        assertFalse(out.contains("\u001b"), out);
        assertFalse(out.contains("\u0007"), out);
        assertTrue(out.contains("\u241B"), "ESC is shown as a visible control picture: " + out);
        assertTrue(out.contains("pwned"), "the text is kept, only the control characters are neutralised");
    }

    @Test
    void carriageReturnsCannotOverwriteALine() {
        assertEquals("safe\u240Dmalicious", TerminalText.sanitize("safe\rmalicious"));
    }

    @Test
    void c1ControlsAndDeleteAreNeutralised() {
        assertEquals("a?b\u2421c", TerminalText.sanitize("a\u009Bb\u007Fc"));
    }

    @Test
    void bidirectionalOverridesThatDisguiseTextAreReplaced() {
        // "Trojan Source": a right-to-left override makes text render differently from how it is stored
        String out = TerminalText.sanitize("if (isAdmin\u202E ) { // \u2066hidden\u2069");
        assertFalse(out.chars().anyMatch(c -> (c >= 0x202A && c <= 0x202E) || (c >= 0x2066 && c <= 0x2069)), out);
    }

    @Test
    void ordinaryTextIncludingUnicodeNewlinesAndTabsIsUntouched() {
        String text = "Olá, mundo! 日本語 ✓ 🚀\n\tindented \"quotes\" <tag> &amp;";
        assertSame(text, TerminalText.sanitize(text), "nothing to change: the same string is returned");
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", TerminalText.sanitize(null));
    }

    @Test
    void theSafePrintStreamFiltersEveryPrintingMethod() {
        var bytes = new ByteArrayOutputStream();
        PrintStream out = SafePrintStream.wrap(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        out.print("a\u001b[31m");
        out.println("b\u001b[0m");
        out.printf("c\u001b[1m%s%n", "d\u001b[2m");
        out.println((Object) "e\u001bf");
        String written = bytes.toString(StandardCharsets.UTF_8);
        assertFalse(written.contains("\u001b"), written);
        assertTrue(written.contains("a\u241B[31mb\u241B[0m"), written);
        assertSame(out, SafePrintStream.wrap(out), "wrapping twice does nothing");
    }

    // ---- command guard ----

    @Test
    void windowsRefusesArgumentsThatCmdExeWouldInterpret() {
        for (String bad : List.of("a & calc", "a | b", "a > out", "a < in", "^x", "%PATH%", "say \"hi\"", "line\nbreak")) {
            assertTrue(CommandGuard.refusal(List.of("mvn", bad), true).contains("cmd.exe"), bad);
        }
    }

    @Test
    void ordinaryArgumentsAreFineOnWindows() {
        assertNull(CommandGuard.refusal(List.of("mvn", "-q", "verify", "-Dtest=FooTest#bar", "C:\\work\\my project\\pom.xml"), true));
    }

    @Test
    void elsewhereShellCharactersAreHarmlessBecauseNoShellIsInvolved() {
        assertNull(CommandGuard.refusal(List.of("echo", "a & b | c > d"), false));
    }

    @Test
    void aNulCharacterIsRefusedOnEveryPlatform() {
        assertTrue(CommandGuard.refusal(List.of("ls", "a\0b"), false).contains("NUL"));
    }
}
