package dev.buildcli.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import dev.buildcli.domain.Capability;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Changes one agent's Markdown file: its role, capabilities, instructions, the folders it may write in and the commands it may
 * run without asking. It rewrites the front matter from what it parsed, so comments in it are lost; the body is kept unless
 * the instructions change. Anything it is given is checked here, because the agent's file is what grants permissions.
 */
public final class AgentFileEditor {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));
    private static final Pattern FRONT = Pattern.compile("\\A---\\R(.*?)\\R---[ \\t]*(?:\\R(.*))?\\z", Pattern.DOTALL);

    /** What to change; null leaves it as it is. A list given, even an empty one, replaces what was there. */
    public record Edit(String role, List<String> capabilities, String instructions, List<String> writeGlobs, List<List<String>> commands) {
        public static Edit role(String role) {
            return new Edit(role, null, null, null, null);
        }

        public static Edit capabilities(List<String> capabilities) {
            return new Edit(null, capabilities, null, null, null);
        }

        public static Edit instructions(String text) {
            return new Edit(null, null, text, null, null);
        }

        public static Edit writeGlobs(List<String> globs) {
            return new Edit(null, null, null, globs, null);
        }

        public static Edit commands(List<List<String>> argv) {
            return new Edit(null, null, null, null, argv);
        }
    }

    private AgentFileEditor() { }

    /** @return the new text of the file; @throws IllegalArgumentException if the file is not a Markdown agent or a value is not allowed */
    public static String apply(String fileText, Edit edit) {
        Matcher m = FRONT.matcher(fileText);
        if (!m.matches()) {
            throw new IllegalArgumentException("this file is not a Markdown agent with a front matter block, so edit it by hand");
        }
        JsonNode parsed;
        try {
            parsed = YAML.readTree(m.group(1));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("its front matter is not valid YAML: " + e.getOriginalMessage());
        }
        if (!(parsed instanceof ObjectNode front)) {
            throw new IllegalArgumentException("its front matter is not a mapping");
        }
        String body = m.group(2) == null ? "" : m.group(2).strip();
        if (edit.role() != null) {
            if (edit.role().isBlank() || edit.role().contains("\n")) {
                throw new IllegalArgumentException("a role is one short line");
            }
            front.put("role", edit.role().strip());
        }
        if (edit.capabilities() != null) {
            List<String> unknown = edit.capabilities().stream().filter(c -> !Capability.KNOWN.contains(c)).toList();
            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException("unknown capabilities " + unknown);
            }
            ArrayNode caps = front.putArray("capabilities");
            edit.capabilities().forEach(caps::add);
        }
        if (edit.instructions() != null) {
            body = edit.instructions().strip();
            front.remove("instructions");
        }
        if (edit.writeGlobs() != null) {
            ArrayNode write = section(front, "filesystem").putArray("write");
            for (String g : edit.writeGlobs()) {
                write.add(checkGlob(g));
            }
        }
        if (edit.commands() != null) {
            ArrayNode allow = section(front, "command").putArray("allow");
            for (List<String> argv : edit.commands()) {
                if (argv.isEmpty() || argv.stream().anyMatch(String::isBlank)) {
                    throw new IllegalArgumentException("a command is a program and its arguments");
                }
                ArrayNode one = allow.addArray();
                argv.forEach(one::add);
            }
        }
        try {
            String yaml = YAML.writeValueAsString(front).stripTrailing();
            return "---\n" + yaml + "\n---\n" + (body.isEmpty() ? "" : body + "\n");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("could not write the file: " + e.getOriginalMessage());
        }
    }

    private static ObjectNode section(ObjectNode front, String name) {
        JsonNode permissions = front.get("permissions");
        ObjectNode p = permissions instanceof ObjectNode o ? o : front.putObject("permissions");
        JsonNode s = p.get(name);
        return s instanceof ObjectNode o ? o : p.putObject(name);
    }

    /** A folder pattern inside the project, never BuildCLI's own files or git's. */
    public static String checkGlob(String glob) {
        String g = glob == null ? "" : glob.strip();
        if (g.isEmpty() || g.startsWith("/") || g.startsWith("~") || g.matches("^[A-Za-z]:.*") || g.contains("..") || g.contains("\\")) {
            throw new IllegalArgumentException("`" + g + "` is not a folder pattern inside the project (like `src/**` or `docs/**`)");
        }
        String lower = g.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith(".buildcli") || lower.startsWith(".git/") || lower.equals(".git") || lower.startsWith("**/.git")) {
            throw new IllegalArgumentException("`" + g + "` would let the agent change BuildCLI's or git's own files");
        }
        return g;
    }
}
