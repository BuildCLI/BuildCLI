package dev.buildcli.infrastructure.tui;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import org.jline.terminal.Terminal;

/**
 * A terminal whose {@code writer()} collects what a frame writes and sends it in one go when the frame is flushed.
 *
 * <p>The UI library draws a frame as hundreds of tiny writes (a colour, a cursor move, a few letters). On Linux and macOS
 * that costs nothing, but in a Windows console every write is a separate call into the console, so a frame that repaints
 * the message area took about 50 ms there against 1.5 ms elsewhere. One write per frame removes that.
 */
final class BufferedTerminal {
    private static final int BUFFER_CHARS = 64 * 1024;

    private BufferedTerminal() {}

    static Terminal wrap(Terminal real) {
        PrintWriter buffered = new PrintWriter(new BufferedWriter(real.writer(), BUFFER_CHARS));
        return (Terminal) Proxy.newProxyInstance(Terminal.class.getClassLoader(), new Class<?>[] {Terminal.class}, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("writer") && method.getParameterCount() == 0) {
                return buffered;
            }
            if (name.equals("flush") || name.equals("close")) {
                buffered.flush(); // whatever is still buffered goes out before the console is released
            }
            try {
                return method.invoke(real, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
