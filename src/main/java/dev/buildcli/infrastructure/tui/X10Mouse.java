package dev.buildcli.infrastructure.tui;

import java.io.IOException;
import java.util.ArrayDeque;

/**
 * Turns the Windows console's X10 mouse reports ({@code ESC [ M} and three bytes) into the SGR form the UI library
 * reads ({@code ESC [ < button ; column ; row M}), and passes every other byte through unchanged. It only reads from a
 * {@link Source}, so it needs no terminal and no thread.
 *
 * <p>The console reports no motion without a button, so a mouse that only moves produces nothing. A button held while
 * the mouse moves is reported as a drag.
 */
final class X10Mouse {
    /** Where bytes come from: a console, or a script in a test. -1 is the end, -2 is "nothing within the timeout". */
    interface Source {
        int read(int timeoutMs) throws IOException;
    }

    private static final int ESC = 27;
    private static final int LOOKAHEAD_MS = 25;
    private final Source source;
    private final ArrayDeque<Integer> ready = new ArrayDeque<>();
    private int held = -1;

    X10Mouse(Source source) {
        this.source = source;
    }

    int read(int timeoutMs) throws IOException {
        return next(timeoutMs);
    }

    int peek(int timeoutMs) throws IOException {
        if (!ready.isEmpty()) {
            return ready.peekFirst();
        }
        int c = next(timeoutMs);
        if (c >= 0) {
            ready.addFirst(c);
        }
        return c;
    }

    private int pull(int timeoutMs) throws IOException {
        return ready.isEmpty() ? source.read(timeoutMs) : ready.poll();
    }

    private int next(int timeoutMs) throws IOException {
        while (true) {
            int c = pull(timeoutMs);
            if (c != ESC) {
                return c;
            }
            int bracket = pull(1);
            if (bracket != '[') {
                if (bracket >= 0) {
                    ready.addFirst(bracket);
                }
                return c;
            }
            int m = pull(1);
            if (m != 'M') {
                if (m >= 0) {
                    ready.addFirst(m);
                }
                ready.addFirst((int) '[');
                return c;
            }
            int button = pull(LOOKAHEAD_MS);
            int column = pull(LOOKAHEAD_MS);
            int row = pull(LOOKAHEAD_MS);
            if (button < 0 || column < 0 || row < 0) {
                continue; // a cut-off mouse report: drop it rather than type its bytes
            }
            String sgr = toSgr(button - 32, column - 32, row - 32);
            if (sgr == null) {
                continue;
            }
            sgr.chars().skip(1).forEach(ready::addLast);
            return ESC;
        }
    }

    /** One X10 report as the SGR sequence for it, or null when it says nothing new (a release with no button held). */
    private String toSgr(int code, int column, int row) {
        if ((code & 64) != 0) {
            return "\u001b[<" + code + ";" + column + ";" + row + "M"; // wheel: 64 up, 65 down
        }
        int button = code & 3;
        if (button == 3) {
            if (held < 0) {
                return null;
            }
            held = -1;
            return "\u001b[<0;" + column + ";" + row + "m";
        }
        if (held < 0) {
            held = button;
            return "\u001b[<" + button + ";" + column + ";" + row + "M";
        }
        return "\u001b[<" + (button | 32) + ";" + column + ";" + row + "M"; // the same button, now moving
    }
}
