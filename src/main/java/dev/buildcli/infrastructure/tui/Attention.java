package dev.buildcli.infrastructure.tui;

/**
 * Decides when to get the user's attention outside the chat: the window title says whether an agent needs you, and the
 * terminal bell rings (which makes most terminals flash the taskbar) when a new question arrives or a long job ends.
 * It only decides; ChatApp applies the result on the thread that draws.
 */
final class Attention {
    /** A job shorter than this is not worth a sound: you were probably watching it. */
    static final long LONG_JOB_MS = 15_000;

    /** @param title the new window title, or null if it did not change */
    record Signal(String title, boolean bell) {}

    private int lastPending;
    private boolean wasBusy;
    private long busySince;
    private String lastTitle = "";

    /** @param agent who asks, when something is pending */
    Signal update(long nowMs, int pending, String agent, boolean busy, boolean bellOn) {
        boolean newQuestion = pending > lastPending;
        boolean longJobEnded = wasBusy && !busy && pending == 0 && nowMs - busySince >= LONG_JOB_MS;
        if (busy && !wasBusy) {
            busySince = nowMs;
        }
        wasBusy = busy;
        lastPending = pending;
        String title = pending > 0 ? "● " + (agent == null || agent.isBlank() ? "An agent" : agent) + " needs you · BuildCLI"
                : busy ? "BuildCLI · working" : "BuildCLI";
        boolean changed = !title.equals(lastTitle);
        lastTitle = title;
        return new Signal(changed ? title : null, bellOn && (newQuestion || longJobEnded));
    }
}
