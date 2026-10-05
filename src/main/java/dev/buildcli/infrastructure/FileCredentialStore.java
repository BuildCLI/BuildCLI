package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * API keys the user typed into BuildCLI, kept in one file in the user's own BuildCLI folder (never in a project), as
 * {@code {"schema":1,"keys":{"OPENROUTER_API_KEY":"..."}}}. They are keyed by the name of the environment variable the
 * provider reads, so a variable that is set always wins and two providers that share a key share the entry.
 *
 * <p>The file is created readable by its owner only (mode 600, or an owner-only ACL on Windows) <em>before</em> the key
 * is written, and replaced atomically. It is plain text, like the credential files of other command line tools: the
 * protection is the file's permissions, and agents are kept away from it by the tools' path rules.
 */
public final class FileCredentialStore {
    public static final String FILE_NAME = "credentials.json";
    private static final Pattern VARIABLE = Pattern.compile("[A-Z_][A-Z0-9_]{0,63}");
    private static final int MAX_KEY_LENGTH = 4096;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path file;
    private Map<String, String> keys = Map.of();
    private boolean damaged;

    public FileCredentialStore(Path file) {
        this.file = file;
        reload();
    }

    public Path file() {
        return file;
    }

    /** Reads the file again. A missing file is empty; a damaged one is empty too, and is never overwritten by accident. */
    public synchronized void reload() {
        damaged = false;
        keys = Map.of();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonNode root = JSON.readTree(Files.readString(file, StandardCharsets.UTF_8));
            Map<String, String> read = new LinkedHashMap<>();
            JsonNode node = root == null ? null : root.path("keys");
            if (node == null || !node.isObject()) {
                damaged = true;
                return;
            }
            node.fields().forEachRemaining(e -> {
                if (VARIABLE.matcher(e.getKey()).matches() && e.getValue().isTextual() && !e.getValue().asText().isBlank()) {
                    read.put(e.getKey(), e.getValue().asText());
                }
            });
            keys = Map.copyOf(read);
        } catch (IOException | RuntimeException e) {
            damaged = true;
        }
    }

    public synchronized Optional<String> get(String variable) {
        return Optional.ofNullable(keys.get(variable));
    }

    public synchronized boolean has(String variable) {
        return keys.containsKey(variable);
    }

    /** The names of the variables with a saved key (never the keys). */
    public synchronized java.util.Set<String> variables() {
        return keys.keySet();
    }

    /** A key as typed or pasted, without the line break or spaces around it. */
    public static String clean(String key) {
        return key == null ? "" : key.strip();
    }

    public static boolean valid(String key) {
        String k = clean(key);
        return !k.isEmpty() && k.length() <= MAX_KEY_LENGTH && k.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c));
    }

    public synchronized void put(String variable, String key) throws IOException {
        String k = clean(key);
        if (!VARIABLE.matcher(variable).matches()) {
            throw new IllegalArgumentException("not an environment variable name: " + variable);
        }
        if (!valid(k)) {
            throw new IllegalArgumentException("that does not look like a key (it is empty, has spaces or line breaks, or is longer than " + MAX_KEY_LENGTH + ")");
        }
        Map<String, String> next = new LinkedHashMap<>(keys);
        next.put(variable, k);
        write(next);
    }

    /** @return false when there was no saved key under that name */
    public synchronized boolean remove(String variable) throws IOException {
        if (!keys.containsKey(variable)) {
            return false;
        }
        Map<String, String> next = new LinkedHashMap<>(keys);
        next.remove(variable);
        write(next);
        return true;
    }

    private void write(Map<String, String> next) throws IOException {
        if (damaged) {
            throw new IOException(file + " cannot be read, so BuildCLI will not overwrite it. Fix or delete it, then try again.");
        }
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        ObjectNode root = JSON.createObjectNode();
        root.put("schema", 1);
        ObjectNode node = root.putObject("keys");
        next.forEach(node::put);
        Path tmp = Files.createTempFile(dir, "credentials-", ".tmp", ownerOnly(dir));
        try {
            restrict(tmp); // before any key is written
            Files.writeString(tmp, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
        keys = Map.copyOf(next);
    }

    /** Mode 600 where the file system has POSIX permissions; nothing otherwise (see {@link #restrict}). */
    private static FileAttribute<?>[] ownerOnly(Path dir) {
        try {
            if (Files.getFileStore(dir).supportsFileAttributeView("posix")) {
                return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))};
            }
        } catch (IOException | RuntimeException e) {
            // fall through: restrict() tries again by other means
        }
        return new FileAttribute<?>[0];
    }

    /** Owner-only on a POSIX file system (again, in case the umask changed it) and on Windows through an ACL. */
    private static void restrict(Path path) throws IOException {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
                return;
            }
        } catch (UnsupportedOperationException e) {
            // not POSIX
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            AclEntry owner = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(Files.getOwner(path))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();
            acl.setAcl(List.of(owner));
        }
    }
}
