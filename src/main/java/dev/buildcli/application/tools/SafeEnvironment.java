package dev.buildcli.application.tools;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The environment handed to commands run for an agent: a small allow list, so API keys and tokens present in the
 * user's shell never reach a process an LLM chose to run.
 */
public final class SafeEnvironment {
    private static final Set<String> ALLOWED = Set.of(
            "PATH", "HOME", "USER", "LOGNAME", "LANG", "TERM", "TMPDIR", "TEMP", "TMP", "JAVA_HOME",
            // Windows needs these to start processes at all
            "SYSTEMROOT", "SYSTEMDRIVE", "USERPROFILE", "PATHEXT", "COMSPEC", "APPDATA", "LOCALAPPDATA");

    private SafeEnvironment() {}

    public static Map<String, String> filter(Map<String, String> environment) {
        Map<String, String> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        environment.forEach((k, v) -> {
            String key = k.toUpperCase(Locale.ROOT);
            if (ALLOWED.contains(key) || key.startsWith("LC_")) {
                out.put(k, v);
            }
        });
        return out;
    }
}
