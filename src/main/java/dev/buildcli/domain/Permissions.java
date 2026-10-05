package dev.buildcli.domain;

import java.time.Duration;
import java.util.List;

/** Per-agent permissions, enforced by the runtime (never by the prompt). */
public record Permissions(List<String> readGlobs, List<String> writeGlobs, List<List<String>> commandAllow,
                          Duration commandTimeout) {

    public static final List<String> READ_EVERYTHING = List.of("**");

    /** Reads default to the whole workspace; writes and commands default to nothing. */
    public Permissions(List<String> writeGlobs, List<List<String>> commandAllow, Duration commandTimeout) {
        this(READ_EVERYTHING, writeGlobs, commandAllow, commandTimeout);
    }

    public static Permissions none() {
        return new Permissions(READ_EVERYTHING, List.of(), List.of(), Duration.ofSeconds(30));
    }
}
