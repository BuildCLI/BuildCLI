package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

/** A frame written as hundreds of small pieces must reach the console as one write. */
class BufferedTerminalTest {

    /** The console side: counts how many separate writes arrive. */
    static final class Console extends Writer {
        final List<String> writes = new ArrayList<>();
        int flushes;
        boolean closed;

        @Override
        public void write(char[] c, int off, int len) {
            writes.add(new String(c, off, len));
        }

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** A terminal with only what is needed: writer(), close() and a method that throws. */
    static Terminal fake(Console console, List<String> calls) {
        java.io.PrintWriter writer = new java.io.PrintWriter(console);
        return (Terminal) Proxy.newProxyInstance(Terminal.class.getClassLoader(), new Class<?>[] {Terminal.class}, (p, m, a) -> {
            calls.add(m.getName());
            return switch (m.getName()) {
                case "writer" -> writer;
                case "getType" -> "windows-vtp";
                case "enterRawMode" -> throw new IllegalStateException("not a terminal");
                default -> null;
            };
        });
    }

    @Test
    void manySmallWritesBecomeOneWriteWhenTheFrameIsFlushed() {
        Console console = new Console();
        Terminal terminal = BufferedTerminal.wrap(fake(console, new ArrayList<>()));
        var out = terminal.writer();
        for (int i = 0; i < 300; i++) {
            out.print("\u001b[" + i + ";1H");
            out.print("x");
        }
        assertEquals(0, console.writes.size(), "nothing reaches the console before the frame is flushed");
        out.flush();
        assertEquals(1, console.writes.size(), "one write for the whole frame");
        int expected = 0;
        for (int i = 0; i < 300; i++) {
            expected += ("\u001b[" + i + ";1H").length() + 1;
        }
        assertEquals(expected, console.writes.get(0).length(), "all of it is there");
    }

    @Test
    void closingSendsWhatIsStillBufferedBeforeTheConsoleIsReleased() {
        Console console = new Console();
        List<String> calls = new ArrayList<>();
        Terminal terminal = BufferedTerminal.wrap(fake(console, calls));
        terminal.writer().print("\u001b[?1049l"); // leaving the alternate screen must not be lost
        try {
            terminal.close();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertEquals(List.of("\u001b[?1049l"), console.writes);
        assertEquals("close", calls.get(calls.size() - 1), "the real terminal is closed after the flush");
    }

    @Test
    void everythingElseGoesStraightToTheRealTerminalAndItsExceptionsKeepTheirType() {
        Terminal terminal = BufferedTerminal.wrap(fake(new Console(), new ArrayList<>()));
        assertEquals("windows-vtp", terminal.getType());
        assertThrows(IllegalStateException.class, terminal::enterRawMode);
        assertSame(terminal.writer(), terminal.writer(), "the same buffered writer every time");
    }
}
