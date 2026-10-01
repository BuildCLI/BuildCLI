package dev.buildcli.domain;

/**
 * One file an agent wrote. {@code before} is the content it replaced (null when the file did not exist, or was too
 * large to keep: then {@code existed} tells which), {@code after} what the agent wrote. Kept so the user can review
 * what an agent did and undo it.
 */
public record FileChange(String agent, String path, boolean existed, String before, String after) {

    /** Undo can restore it: its content was kept (it is not after a restart for sensitive or very large files), and the file was new or its old content was kept. */
    public boolean restorable() {
        return after != null && (!existed || before != null);
    }
}
