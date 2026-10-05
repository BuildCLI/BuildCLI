package dev.buildcli.application;

import java.util.Locale;

/**
 * How much the agents may do before asking you. It only decides which questions are answered for you: what an agent is
 * allowed to touch is still set by its own permissions, and the question that trusts a project is never skipped.
 */
public enum ApprovalMode {
    /** Every write, command outside the allow list and commit asks you first. */
    MANUAL("manual", "asks before every write, command and commit"),
    /** Writes to files the agent may change are approved for you (you can still undo them); commands and commits ask. */
    EDITS("edits", "writes files without asking (undo is one click away); commands and commits still ask"),
    /** Writes, commands outside the allow list and commits are all approved for you. */
    AUTO("auto", "writes, runs commands and commits without asking: use it where nothing can be lost");

    private final String label;
    private final String description;

    ApprovalMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** Whether a question of this kind ({@code write}, {@code command}, {@code git_commit}, {@code trust}) is answered yes for you. */
    public boolean approves(String kind) {
        return switch (this) {
            case MANUAL -> false;
            case EDITS -> "write".equals(kind);
            case AUTO -> "write".equals(kind) || "command".equals(kind) || "git_commit".equals(kind);
        };
    }

    public ApprovalMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** "manual", "edits" or "auto"; anything else is manual, the safe one. */
    public static ApprovalMode parse(String text) {
        if (text != null) {
            String t = text.strip().toLowerCase(Locale.ROOT);
            for (ApprovalMode m : values()) {
                if (m.label.equals(t)) {
                    return m;
                }
            }
        }
        return MANUAL;
    }
}
