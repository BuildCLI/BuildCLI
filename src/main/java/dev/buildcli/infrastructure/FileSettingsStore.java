package dev.buildcli.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.buildcli.ports.SettingsStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** {@code settings.yaml} in the global directory and in the project's state directory (never the project tree). */
public final class FileSettingsStore implements SettingsStore {
    public static final String FILE_NAME = "settings.yaml";
    private static final long MAX_BYTES = 256L * 1024;
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private final Path global;
    private final Path project;

    public FileSettingsStore(Path globalDir, Path projectStateDir) {
        this.global = globalDir.resolve(FILE_NAME);
        this.project = projectStateDir.resolve(FILE_NAME);
    }

    private Path file(Scope scope) {
        return scope == Scope.GLOBAL ? global : project;
    }

    @Override
    public Map<String, String> load(Scope scope) {
        Path f = file(scope);
        try {
            if (!Files.isRegularFile(f) || Files.size(f) > MAX_BYTES) {
                return Map.of();
            }
            Map<String, Object> raw = YAML.readValue(f.toFile(), new TypeReference<Map<String, Object>>() {});
            Map<String, String> out = new TreeMap<>();
            if (raw != null) {
                raw.forEach((k, v) -> {
                    if (v != null && !(v instanceof Map) && !(v instanceof Iterable)) {
                        out.put(k, v.toString());
                    }
                });
            }
            return out;
        } catch (IOException e) {
            return Map.of(); // a broken settings file must not stop the app; the defaults apply
        }
    }

    @Override
    public void save(Scope scope, Map<String, String> values) {
        Path f = file(scope);
        try {
            Files.createDirectories(f.getParent());
            YAML.writeValue(f.toFile(), new TreeMap<>(values));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot save " + f + ": " + e.getMessage(), e);
        }
    }
}
