package dev.buildcli.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Where BuildCLI keeps things. Operational state (runs, tasks, events) lives under the user's home, keyed by project,
 * and is never written into the project tree.
 *
 * <pre>
 * ~/.buildcli/                         global agents (override with BUILDCLI_HOME)
 * ~/.buildcli/projects/&lt;id&gt;/state.db   runs, tasks and events of one project
 * </pre>
 */
public final class StateLocations {
    public static final String HOME_VARIABLE = "BUILDCLI_HOME";

    private StateLocations() {}

    /** {@code $BUILDCLI_HOME} if set, otherwise {@code ~/.buildcli}. */
    public static Path globalDir(Map<String, String> env, Path userHome) {
        String override = env.get(HOME_VARIABLE);
        return override != null && !override.isBlank() ? Path.of(override) : userHome.resolve(".buildcli");
    }

    /** A stable, readable, collision-resistant directory for one project: {@code <name>-<12 hex of sha256(path)>}. */
    public static Path projectStateDir(Path globalDir, Path projectDir) {
        Path canonical = canonical(projectDir);
        Path fileName = canonical.getFileName();
        String name = fileName == null ? "root" : fileName.toString().toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        return globalDir.resolve("projects").resolve(name + "-" + hash(canonical.toString()));
    }

    public static Path stateDb(Path globalDir, Path projectDir) {
        return projectStateDir(globalDir, projectDir).resolve("state.db");
    }

    private static Path canonical(Path p) {
        try {
            return p.toRealPath();
        } catch (IOException e) {
            return p.toAbsolutePath().normalize();
        }
    }

    private static String hash(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
