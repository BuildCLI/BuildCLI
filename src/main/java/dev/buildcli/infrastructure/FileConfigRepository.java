package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.domain.Origin;
import dev.buildcli.domain.Permissions;
import dev.buildcli.ports.ConfigException;
import dev.buildcli.ports.ConfigRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads agents ({@code agents/*.md|yaml}) from the global directory and from the
 * project's {@code .buildcli/}; project definitions override global ones of the same name. Definitions are validated
 * strictly (unknown keys are errors, since a typo in a permission must never silently widen or drop it) and every
 * problem is reported at once. {@code AGENTS.md} in the project root is read as context only.
 *
 * <p>File format, schema 1: see docs/reference/agents.md.
 */
public final class FileConfigRepository implements ConfigRepository {
    public static final int SCHEMA = 1;
    public static final int MAX_CONTEXT_CHARS = 8000;
    /** Definition files are small; a huge one in a cloned project is refused instead of being read into memory. */
    public static final long MAX_FILE_BYTES = 256L * 1024;

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_-]*");
    private static final Pattern DURATION = Pattern.compile("(\\d+)(ms|s|m|h)");
    /** A provider name; whether it exists is checked against the provider registry when a run starts. */
    private static final java.util.regex.Pattern PROVIDER = java.util.regex.Pattern.compile("[a-z][a-z0-9-]{0,39}");
    private static final Set<String> AGENT_KEYS = Set.of("schema", "name", "role", "description", "instructions",
            "capabilities", "permissions", "memory", "personality");

    private final Map<String, Agent> agents = new LinkedHashMap<>();
    private final String context;
    private final java.security.MessageDigest digest = sha256();
    private boolean projectFilesSeen;
    private final Path projectRoot;
    private String projectDigest = "";
    private final List<String> problems = new ArrayList<>();

    /**
     * @param projectDir the project root (holds {@code .buildcli/} and {@code AGENTS.md})
     * @param globalDir  the user-level directory, normally {@code ~/.buildcli}; may not exist
     * @throws ConfigException listing every invalid file
     */
    public FileConfigRepository(Path projectDir, Path globalDir) {
        Path project = projectDir.toAbsolutePath().normalize();
        this.projectRoot = project;
        loadAgents(globalDir.resolve("agents"), Origin.GLOBAL);
        loadAgents(project.resolve(".buildcli").resolve("agents"), Origin.PROJECT);
        context = readContext(project.resolve("AGENTS.md"));
        projectDigest = projectFilesSeen ? java.util.HexFormat.of().formatHex(digest.digest()) : "";
        if (!problems.isEmpty()) {
            throw new ConfigException(problems);
        }
    }

    @Override
    public List<Agent> agents() {
        return List.copyOf(agents.values());
    }

    @Override
    public Optional<Agent> agent(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    @Override
    public String projectDigest() {
        return projectDigest;
    }

    @Override
    public String projectContext() {
        return context;
    }

    // ---- agents ----

    private void loadAgents(Path dir, Origin origin) {
        Map<String, Path> seen = new LinkedHashMap<>();
        for (Path file : files(dir, ".md", ".yaml", ".yml")) {
            Agent a = parseAgent(file, origin);
            if (a == null) {
                continue;
            }
            Path clash = seen.put(a.name(), file);
            if (clash != null) {
                problem(file, "agent '" + a.name() + "' is also defined in " + clash.getFileName());
                continue;
            }
            agents.put(a.name(), a); // a project definition replaces a global one of the same name
        }
    }

    private Agent parseAgent(Path file, Origin origin) {
        String text = read(file);
        if (text == null) {
            return null;
        }
        String body = "";
        String yaml = text;
        if (file.getFileName().toString().endsWith(".md")) {
            Matcher m = Pattern.compile("\\A---\\R(.*?)\\R---[ \\t]*(?:\\R(.*))?\\z", Pattern.DOTALL).matcher(text);
            if (!m.matches()) {
                problem(file, "a Markdown agent must start with a YAML front matter block delimited by '---' lines");
                return null;
            }
            yaml = m.group(1);
            body = m.group(2) == null ? "" : m.group(2).strip();
        }
        JsonNode n = parseYaml(file, yaml);
        if (n == null) {
            return null;
        }
        int before = problems.size();
        checkSchema(file, n);
        checkKeys(file, n, AGENT_KEYS, "agent");
        String name = requiredText(file, n, "name");
        String role = requiredText(file, n, "role");
        if (name != null && !NAME.matcher(name).matches()) {
            problem(file, "name '" + name + "' must match [a-z][a-z0-9_-]*");
        }
        String instructions = optionalText(file, n, "instructions");
        if (!body.isEmpty()) {
            instructions = instructions.isEmpty() ? body : instructions.strip() + "\n\n" + body;
        }
        Set<String> capabilities = capabilities(file, n.get("capabilities"));
        Permissions permissions = permissions(file, n.get("permissions"));
        if (problems.size() > before) {
            return null;
        }
        return new Agent(name, role, instructions, capabilities, permissions, origin, file.toString());
    }

    private Set<String> capabilities(Path file, JsonNode node) {
        Set<String> out = new TreeSet<>();
        if (node == null || node.isNull()) {
            return out;
        }
        if (!node.isArray()) {
            problem(file, "'capabilities' must be a list");
            return out;
        }
        for (JsonNode c : node) {
            String cap = c.isTextual() ? c.asText() : null;
            if (cap == null || !Capability.KNOWN.contains(cap)) {
                problem(file, "unknown capability " + c + "; known capabilities: " + new TreeSet<>(Capability.KNOWN));
            } else {
                out.add(cap);
            }
        }
        return out;
    }

    private Permissions permissions(Path file, JsonNode node) {
        List<String> read = Permissions.READ_EVERYTHING;
        List<String> write = List.of();
        List<List<String>> allow = List.of();
        Duration timeout = Duration.ofSeconds(30);
        if (node == null || node.isNull()) {
            return new Permissions(read, write, allow, timeout);
        }
        if (!node.isObject()) {
            problem(file, "'permissions' must be a mapping");
            return null;
        }
        for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
            String k = it.next();
            if (k.equals("network")) {
                problem(file, "permissions.network is not supported in 1.0: it cannot be enforced without the command sandbox"
                        + " planned for 1.2, and an unenforced permission would be misleading");
            } else if (!k.equals("filesystem") && !k.equals("command")) {
                problem(file, "unknown key permissions." + k + " (expected: filesystem, command)");
            }
        }
        JsonNode fs = node.get("filesystem");
        if (fs != null && fs.isObject()) {
            unknownKeys(file, fs, Set.of("read", "write"), "permissions.filesystem");
            if (fs.has("read")) {
                read = stringList(file, fs.get("read"), "permissions.filesystem.read");
            }
            if (fs.has("write")) {
                write = stringList(file, fs.get("write"), "permissions.filesystem.write");
            }
        } else if (fs != null) {
            problem(file, "'permissions.filesystem' must be a mapping");
        }
        JsonNode cmd = node.get("command");
        if (cmd != null && cmd.isObject()) {
            unknownKeys(file, cmd, Set.of("allow", "timeout"), "permissions.command");
            allow = argvList(file, cmd.get("allow"));
            if (cmd.has("timeout")) {
                timeout = duration(file, cmd.get("timeout"));
            }
        } else if (cmd != null) {
            problem(file, "'permissions.command' must be a mapping");
        }
        return new Permissions(read, write, allow, timeout);
    }

    private List<List<String>> argvList(Path file, JsonNode node) {
        List<List<String>> out = new ArrayList<>();
        if (node == null || node.isNull()) {
            return out;
        }
        if (!node.isArray()) {
            problem(file, "'permissions.command.allow' must be a list of argv arrays, e.g. [[\"mvn\", \"test\"]]");
            return out;
        }
        for (JsonNode entry : node) {
            if (!entry.isArray() || entry.isEmpty()) {
                problem(file, "command allow entry " + entry + " must be a non-empty array of strings (argv), not a shell string");
                continue;
            }
            List<String> argv = new ArrayList<>();
            for (JsonNode part : entry) {
                if (!part.isTextual()) {
                    problem(file, "command allow entry " + entry + " must contain only strings");
                    break;
                }
                argv.add(part.asText());
            }
            out.add(List.copyOf(argv));
        }
        return out;
    }

    private Duration duration(Path file, JsonNode node) {
        Matcher m = DURATION.matcher(node.asText());
        if (!m.matches()) {
            problem(file, "invalid timeout '" + node.asText() + "'; use e.g. 500ms, 30s, 10m or 1h");
            return Duration.ofSeconds(30);
        }
        long v = Long.parseLong(m.group(1));
        return switch (m.group(2)) {
            case "ms" -> Duration.ofMillis(v);
            case "s" -> Duration.ofSeconds(v);
            case "m" -> Duration.ofMinutes(v);
            default -> Duration.ofHours(v);
        };
    }


    // ---- shared helpers ----

    private String readContext(Path agentsMd) {
        if (!Files.isRegularFile(agentsMd)) {
            return "";
        }
        String text = read(agentsMd);
        if (text == null) {
            return "";
        }
        text = text.strip();
        return text.length() <= MAX_CONTEXT_CHARS ? text
                : text.substring(0, MAX_CONTEXT_CHARS) + "\n[AGENTS.md truncated at " + MAX_CONTEXT_CHARS + " characters]";
    }

    private boolean isProjectDefinition(Path file) {
        return file.toAbsolutePath().normalize().startsWith(projectRoot.resolve(".buildcli"));
    }

    private static java.security.MessageDigest sha256() {
        try {
            return java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Path> files(Path dir, String... extensions) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String f = p.getFileName().toString();
                        return List.of(extensions).stream().anyMatch(f::endsWith);
                    })
                    .sorted().toList();
        } catch (IOException e) {
            problem(dir, "cannot list directory: " + e.getMessage());
            return List.of();
        }
    }

    private String read(Path file) {
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                problem(file, "file is larger than " + MAX_FILE_BYTES / 1024 + " KB; configuration files are small, refusing to read it");
                return null;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (isProjectDefinition(file)) {
                digest.update((projectRoot.relativize(file).toString().replace('\\', '/') + "\0" + text + "\0")
                        .getBytes(StandardCharsets.UTF_8));
                projectFilesSeen = true;
            }
            return text;
        } catch (IOException e) {
            problem(file, "cannot read file: " + e.getMessage());
            return null;
        }
    }

    private JsonNode parseYaml(Path file, String yaml) {
        try {
            JsonNode n = YAML.readTree(yaml);
            if (n == null || !n.isObject()) {
                problem(file, "expected a YAML mapping at the top level");
                return null;
            }
            return n;
        } catch (IOException e) {
            problem(file, "invalid YAML: " + firstLine(e.getMessage()));
            return null;
        }
    }

    private void checkSchema(Path file, JsonNode n) {
        JsonNode v = n.get("schema");
        if (v == null) {
            problem(file, "missing 'schema' (add 'schema: " + SCHEMA + "')");
        } else if (!v.isInt() || v.asInt() != SCHEMA) {
            problem(file, "unsupported schema " + v + "; this BuildCLI reads schema " + SCHEMA);
        }
    }

    private void checkKeys(Path file, JsonNode n, Set<String> allowed, String what) {
        unknownKeys(file, n, allowed, what);
    }

    private void unknownKeys(Path file, JsonNode n, Set<String> allowed, String where) {
        for (Iterator<String> it = n.fieldNames(); it.hasNext();) {
            String k = it.next();
            if (!allowed.contains(k)) {
                problem(file, "unknown key '" + k + "' in " + where + " (allowed: " + new TreeSet<>(allowed) + ")");
            }
        }
    }

    private String requiredText(Path file, JsonNode n, String key) {
        JsonNode v = n.get(key);
        if (v == null || !v.isTextual() || v.asText().isBlank()) {
            problem(file, "'" + key + "' is required and must be a non-empty string");
            return null;
        }
        return v.asText().strip();
    }

    private String optionalText(Path file, JsonNode n, String key) {
        JsonNode v = n.get(key);
        if (v == null || v.isNull()) {
            return "";
        }
        if (!v.isTextual()) {
            problem(file, "'" + key + "' must be a string");
            return "";
        }
        return v.asText();
    }

    private List<String> stringList(Path file, JsonNode node, String where) {
        List<String> out = new ArrayList<>();
        if (!node.isArray()) {
            problem(file, "'" + where + "' must be a list of glob strings");
            return out;
        }
        for (JsonNode v : node) {
            if (v.isTextual()) {
                try {
                    java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + v.asText());
                    out.add(v.asText());
                } catch (java.util.regex.PatternSyntaxException | UnsupportedOperationException e) {
                    problem(file, "'" + where + "' has an invalid glob '" + v.asText() + "': " + firstLine(e.getMessage()));
                }
            } else {
                problem(file, "'" + where + "' must contain only strings");
            }
        }
        return out;
    }

    private void problem(Path file, String message) {
        problems.add(file + ": " + message);
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }
}
