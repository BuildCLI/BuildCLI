package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which clipboard tool each system gets, and what the OSC 52 fallback sends. */
class ClipboardTest {
    static List<String> names(String os, boolean wsl, String... present) {
        Set<String> have = Set.of(present);
        return Clipboard.candidates(os, wsl, have::contains).stream().map(t -> String.join(" ", t.argv())).toList();
    }

    @Test
    void windowsUsesClipWithUtf16AndNothingElse() {
        var tools = Clipboard.candidates("Windows 11", false, n -> n.equals("clip.exe"));
        assertEquals(1, tools.size());
        assertEquals(List.of("clip.exe"), tools.get(0).argv());
        assertTrue(tools.get(0).utf16WithBom(), "clip.exe turns UTF-8 accents into garbage");
        assertEquals(List.of(), names("Windows 11", false, "xclip", "pbcopy"), "no Linux tools on Windows");
    }

    @Test
    void macUsesPbcopy() {
        assertEquals(List.of("pbcopy"), names("Mac OS X", false, "pbcopy", "xclip"));
    }

    @Test
    void linuxPrefersWaylandThenXclipThenXsel() {
        assertEquals(List.of("wl-copy", "xclip -selection clipboard", "xsel --clipboard --input"),
                names("Linux", false, "wl-copy", "xclip", "xsel"));
        assertEquals(List.of("xsel --clipboard --input"), names("Linux", false, "xsel"));
        assertEquals(List.of(), names("Linux", false), "nothing installed: the terminal sequence is the fallback");
    }

    @Test
    void wslReachesTheWindowsClipboardThroughClipExe() {
        assertEquals(List.of("clip.exe", "xclip -selection clipboard"), names("Linux", true, "clip.exe", "xclip"));
    }

    @Test
    void osc52CarriesTheTextAsBase64WithoutTouchingItsBytes() {
        assertEquals("\u001b]52;c;aGVsbG8=\u0007", Clipboard.osc52("hello"));
        String accented = Clipboard.osc52("olá ✓");
        assertEquals("olá ✓", new String(java.util.Base64.getDecoder().decode(accented.substring(7, accented.length() - 1)),
                java.nio.charset.StandardCharsets.UTF_8));
    }
}
