package dev.buildcli.ports;

import java.util.List;

/** Invalid or unreadable agent configuration. Carries every problem found, not just the first. */
public final class ConfigException extends RuntimeException {
    private final List<String> problems;

    public ConfigException(List<String> problems) {
        super(String.join("\n", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
