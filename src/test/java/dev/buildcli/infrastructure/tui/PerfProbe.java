package dev.buildcli.infrastructure.tui;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.tamboui.backend.jline3.JLineBackend;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Not a test: measures what one frame costs (time, bytes and write calls) on a realistic chat. Run by hand. */
public final class PerfProbe {
    static final AtomicLong BYTES = new AtomicLong();
    static final AtomicLong WRITES = new AtomicLong();

    public static void main(String[] args) throws Exception {
        int w = args.length > 0 ? Integer.parseInt(args[0]) : 120;
        int h = args.length > 1 ? Integer.parseInt(args[1]) : 40;
        var session = new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "Here is **a plan** for `" + request.text() + "`.\n\n- first step with a fairly long line that wraps around the bubble when it is narrow enough\n- second step\n\n```java\nclass A {\n    int x = 1;\n}\n```\n\nDone.";
            return t;
        });
        var screen = new ChatScreen(session, Map.of("ana", "m", "bruno", "m"), Files.createTempDirectory("perf"), () -> { });
        for (int i = 0; i < 12; i++) {
            session.submit("question number " + i);
            long end = System.nanoTime() + 5_000_000_000L;
            while ((session.busy() || session.queued() > 0) && System.nanoTime() < end) {
                Thread.sleep(5);
            }
        }
        OutputStream counting = new OutputStream() {
            @Override
            public void write(int b) {
                BYTES.incrementAndGet();
                WRITES.incrementAndGet();
            }

            @Override
            public void write(byte[] b, int off, int len) {
                BYTES.addAndGet(len);
                WRITES.incrementAndGet();
            }
        };
        boolean real = args.length > 2 && args[2].equals("real");
        java.io.PrintStream report = real ? new java.io.PrintStream(new java.io.FileOutputStream(args[3]), true) : System.out;
        PerfProbe.out = report;
        JLineBackend backend;
        if (real && args.length > 4 && args[4].equals("buffered")) {
            backend = new JLineBackend(BufferedTerminal.wrap(org.jline.terminal.TerminalBuilder.builder().system(true).jansi(true).build()));
            backend.enableRawMode();
            backend.enterAlternateScreen();
        } else if (real) {
            backend = new JLineBackend(); // the real console: this is what a user's terminal receives
            backend.enableRawMode();
            backend.enterAlternateScreen();
        } else {
            var in = new java.io.PipedInputStream(new java.io.PipedOutputStream(), 1024);
            var jline = org.jline.terminal.TerminalBuilder.builder().system(false).type("xterm-256color").streams(in, counting)
                    .size(new org.jline.terminal.Size(w, h)).build();
            backend = new JLineBackend(jline);
        }
        out.println("terminal: " + backend.jlineTerminal().getClass().getSimpleName() + " type=" + backend.jlineTerminal().getType()
                + " size=" + backend.jlineTerminal().getSize().getColumns() + "x" + backend.jlineTerminal().getSize().getRows());
        var terminal = new Terminal<>(backend);
        Runnable frame = () -> terminal.draw(f -> screen.render(f, f.area(), RenderContext.empty()));
        report("first frame", frame);
        for (int i = 0; i < 20; i++) {
            frame.run(); // warm up the JIT, as a real session would be
        }
        report("same screen again", frame);
        report("same screen again", frame);
        for (char c : "hello world".toCharArray()) {
            screen.handleKeyEvent(KeyEvent.ofChar(c), true);
            report("one key typed '" + c + "'", frame);
        }
        screen.handleKeyEvent(KeyEvent.ofKey(KeyCode.PAGE_UP), true);
        report("PageUp (scroll 10 lines)", frame);
        screen.handleKeyEvent(KeyEvent.ofKey(KeyCode.PAGE_UP), true);
        report("PageUp again", frame);
        long t0 = System.nanoTime();
        for (int i = 0; i < 200; i++) {
            frame.run();
        }
        out.printf("%n200 unchanged frames: %.2f ms each%n", (System.nanoTime() - t0) / 200 / 1e6);
        // scrolling, as the wheel does it: many frames in a row, each one repainting the message area
        for (int round = 0; round < 3; round++) {
            long t1 = System.nanoTime();
            int frames = 0;
            for (int i = 0; i < 20; i++) {
                screen.handleKeyEvent(KeyEvent.ofKey(KeyCode.PAGE_UP), true);
                frame.run();
                screen.handleKeyEvent(KeyEvent.ofKey(KeyCode.PAGE_DOWN), true);
                frame.run();
                frames += 2;
            }
            out.printf("scroll burst %d: %d frames, %.1f ms per frame%n", round + 1, frames, (System.nanoTime() - t1) / frames / 1e6);
        }
        backend.leaveAlternateScreen();
        out.println("done");
    }

    static java.io.PrintStream out = System.out;

    static void report(String what, Runnable frame) {
        BYTES.set(0);
        WRITES.set(0);
        long t0 = System.nanoTime();
        frame.run();
        out.printf("%-28s %7.2f ms  %7d bytes  %5d write calls%n", what, (System.nanoTime() - t0) / 1e6, BYTES.get(), WRITES.get());
    }
}
