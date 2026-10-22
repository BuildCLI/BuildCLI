package dev.buildcli.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.buildcli.ports.TrustStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;

/**
 * JSON file mapping a project to the digest the user approved (normally {@code ~/.buildcli/trust.json}). An unreadable
 * or corrupt file is treated as "nothing is trusted", the safe direction, and is replaced on the next approval.
 */
public final class FileTrustStore implements TrustStore {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path file;

    public FileTrustStore(Path file) {
        this.file = file;
    }

    @Override
    public synchronized boolean isTrusted(String projectKey, String digest) {
        return !digest.isEmpty() && digest.equals(read().get(projectKey));
    }

    @Override
    public synchronized void trust(String projectKey, String digest) {
        Map<String, String> all = read();
        all.put(projectKey, digest);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = Files.createTempFile(file.toAbsolutePath().getParent(), "trust", ".tmp");
            JSON.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), all);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("cannot save " + file + ": " + e.getMessage(), e);
        }
    }

    private Map<String, String> read() {
        if (!Files.isRegularFile(file)) {
            return new TreeMap<>();
        }
        try {
            return new TreeMap<>(JSON.readValue(file.toFile(), new TypeReference<Map<String, String>>() {}));
        } catch (IOException | RuntimeException e) {
            return new TreeMap<>();
        }
    }
}
