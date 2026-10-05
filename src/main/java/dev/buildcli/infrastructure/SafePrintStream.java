package dev.buildcli.infrastructure;

import java.io.PrintStream;

/** A PrintStream that passes everything printed through {@link TerminalText#sanitize}. */
public final class SafePrintStream extends PrintStream {

    private SafePrintStream(PrintStream target) {
        super(target, true);
    }

    /** Wraps a stream unless it is already safe. */
    public static PrintStream wrap(PrintStream target) {
        return target instanceof SafePrintStream ? target : new SafePrintStream(target);
    }

    @Override
    public void print(String s) {
        super.print(TerminalText.sanitize(s));
    }

    @Override
    public void print(Object obj) {
        super.print(TerminalText.sanitize(String.valueOf(obj)));
    }

    @Override
    public void print(char[] s) {
        super.print(TerminalText.sanitize(new String(s)));
    }
}
