package dev.buildcli.infrastructure;

/**
 * Makes untrusted text safe to show on a terminal. Model replies, file contents and command output can contain control
 * characters and escape sequences that move the cursor, clear or rewrite the screen, or set the window title, and
 * bidirectional overrides that make text read differently from how it is stored ("Trojan Source"). Shown unfiltered in an
 * approval dialog, they could make a dangerous change look harmless. Such characters are replaced by visible symbols;
 * newlines and tabs are kept.
 */
public final class TerminalText {
    private TerminalText() {}

    public static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char replacement = replacementFor(c);
            if (replacement != c) {
                if (out == null) {
                    out = new StringBuilder(text.length()).append(text, 0, i);
                }
                out.append(replacement);
            } else if (out != null) {
                out.append(c);
            }
        }
        return out == null ? text : out.toString();
    }

    private static char replacementFor(char c) {
        if (c == '\n' || c == '\t') {
            return c;
        }
        if (c == '\r') {
            return '␍'; // a bare carriage return can overwrite the start of a line
        }
        if (c < 0x20) {
            return (char) (0x2400 + c); // control pictures: ESC shows as a visible symbol
        }
        if (c == 0x7F) {
            return '␡';
        }
        if (c >= 0x80 && c <= 0x9F) {
            return '?'; // C1 controls, including the 8-bit CSI
        }
        if ((c >= '\u202A' && c <= '\u202E') || (c >= '\u2066' && c <= '\u2069') || c == '\u200E' || c == '\u200F'
                || c == '\u061C') {
            return '?'; // bidirectional overrides, embeddings, isolates and marks
        }
        return c;
    }
}
