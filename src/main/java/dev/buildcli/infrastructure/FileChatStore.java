package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.buildcli.domain.Chat;
import dev.buildcli.ports.ChatStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** {@code chats.yaml} in the project's state directory. A broken file means no saved groups, never a crash. */
public final class FileChatStore implements ChatStore {
    public static final String FILE_NAME = "chats.yaml";
    public static final String REACH_FILE_NAME = "reach.yaml";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    /** How the main group (whose id is empty) is written in the file; files from before it was renamed say "~team". */
    private static final String MAIN_KEY = "~main";
    private static final String OLD_MAIN_KEY = "~team";
    private final Path file;
    private final Path reachFile;

    public FileChatStore(Path projectStateDir) {
        this.file = projectStateDir.resolve(FILE_NAME);
        this.reachFile = projectStateDir.resolve(REACH_FILE_NAME);
    }

    @Override
    public List<Chat> load() {
        List<Chat> out = new ArrayList<>();
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > 512 * 1024) {
                return out;
            }
            JsonNode root = YAML.readTree(file.toFile());
            for (JsonNode g : root.path("groups")) {
                List<String> members = new ArrayList<>();
                g.path("members").forEach(m -> members.add(m.asText()));
                List<String> admins = new ArrayList<>();
                g.path("admins").forEach(m -> admins.add(m.asText()));
                String raw = g.path("id").asText("");
                String id = raw.equals(MAIN_KEY) || raw.equals(OLD_MAIN_KEY) ? "" : raw;
                List<String> files = new ArrayList<>();
                g.path("files").forEach(f -> files.add(f.asText()));
                if (!raw.isBlank()) {
                    out.add(new Chat(id, g.path("name").asText(id), true, members, admins, g.path("context").asText(""), files));
                }
            }
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        return out;
    }

    @Override
    public void save(List<Chat> groups) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Chat c : groups) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.id().isEmpty() ? MAIN_KEY : c.id());
            m.put("name", c.name());
            m.put("members", c.members());
            m.put("admins", c.admins());
            if (!c.context().isEmpty()) {
                m.put("context", c.context());
            }
            if (!c.files().isEmpty()) {
                m.put("files", c.files());
            }
            list.add(m);
        }
        try {
            Files.createDirectories(file.getParent());
            YAML.writeValue(file.toFile(), Map.of("schema", 1, "groups", list));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot save " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, List<String>> loadBlocked() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        try {
            if (!Files.isRegularFile(reachFile) || Files.size(reachFile) > 64 * 1024) {
                return out;
            }
            JsonNode root = YAML.readTree(reachFile.toFile());
            root.path("cannot_contact").fields().forEachRemaining(e -> {
                List<String> names = new ArrayList<>();
                e.getValue().forEach(n -> names.add(n.asText()));
                if (!names.isEmpty()) {
                    out.put(e.getKey(), names);
                }
            });
        } catch (IOException | RuntimeException e) {
            return new LinkedHashMap<>();
        }
        return out;
    }

    @Override
    public void saveBlocked(Map<String, List<String>> blocked) {
        try {
            Files.createDirectories(reachFile.getParent());
            YAML.writeValue(reachFile.toFile(), Map.of("schema", 1, "cannot_contact", blocked));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot save " + reachFile + ": " + e.getMessage(), e);
        }
    }
}
