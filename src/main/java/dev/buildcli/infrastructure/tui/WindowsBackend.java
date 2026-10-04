package dev.buildcli.infrastructure.tui;

import dev.tamboui.backend.jline3.JLineBackend;
import java.io.IOException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * The backend for a native Windows console, which needs two things the UI library does not do there.
 *
 * <p><b>Speed.</b> A frame is hundreds of tiny writes, and every write into a Windows console is a separate call, so a
 * frame that repaints the message area took about 50 ms against 1.5 ms on Linux. {@link BufferedTerminal} sends each frame
 * in one write (measured: 48 ms down to 1 ms).
 *
 * <p><b>Mouse.</b> The terminal library only hands mouse events to a program that asked for them through
 * {@code trackMouse}, which the UI library never does, so clicks and the wheel did nothing. It then reports them in the
 * old X10 form, while the UI library reads the SGR form; {@link X10Mouse} converts one into the other.
 */
final class WindowsBackend extends JLineBackend {
    private final Terminal terminal;
    private final X10Mouse mouse = new X10Mouse(super::read);

    WindowsBackend(Terminal terminal, boolean trackMouse) {
        super(terminal);
        this.terminal = terminal;
        if (trackMouse) {
            terminal.trackMouse(Terminal.MouseTracking.Button);
        }
    }

    /** A backend on the system console, or null if the console cannot be opened (the default backend then reports why). */
    static WindowsBackend open(boolean trackMouse) {
        try {
            return new WindowsBackend(BufferedTerminal.wrap(TerminalBuilder.builder().system(true).jansi(true).build()), trackMouse);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    @Override
    public int read(int timeout) throws IOException {
        return mouse.read(timeout);
    }

    @Override
    public int peek(int timeout) throws IOException {
        return mouse.peek(timeout);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            terminal.close();
        }
    }
}
