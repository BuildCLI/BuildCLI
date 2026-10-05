package dev.buildcli.ports;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * What the git tools need from a repository: status, diff, log and a commit of named paths. Paths are relative to the
 * workspace and are never patterns. Failures that are the repository's (not a repository, nothing to commit, a hook that
 * said no) are reported as an {@link IOException} whose message can be shown as it is.
 */
public interface GitAccess {
    /** What a diff compares. */
    enum Scope {
        /** The working tree against the index: what {@code git diff} shows. */
        WORKTREE,
        /** The index against the last commit: {@code git diff --staged}. */
        STAGED,
        /** The working tree against the last commit: {@code git diff HEAD}. */
        HEAD
    }

    /** {@code git status --short}, with the {@code ## branch} line first when {@code branch} is set; limited to {@code paths} unless empty. */
    String status(Path workspace, boolean branch, List<String> paths) throws IOException;

    /** A unified diff of {@code paths} (all changes when empty). */
    String diff(Path workspace, Scope scope, List<String> paths) throws IOException;

    /** The last {@code max} commits, one line each ({@code abbreviated-id subject}), with branch and tag names when {@code decorate}. */
    String log(Path workspace, int max, boolean decorate) throws IOException;

    /** Stages {@code paths} and commits only them. Hooks run as they would for the user. Returns {@code [branch id] subject}. */
    String commit(Path workspace, String message, List<String> paths) throws IOException;
}
