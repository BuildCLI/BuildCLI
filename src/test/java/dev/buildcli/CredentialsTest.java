package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.Redactor;
import dev.buildcli.application.tools.ListFilesTool;
import dev.buildcli.application.tools.ReadFileTool;
import dev.buildcli.application.tools.SearchTool;
import dev.buildcli.application.tools.ToolContext;
import dev.buildcli.application.tools.WriteFileTool;
import dev.buildcli.cli.BuildCli;
import dev.buildcli.cli.CliContext;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Permissions;
import dev.buildcli.infrastructure.FileCredentialStore;
import dev.buildcli.ports.ToolCall;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** API keys typed into BuildCLI: where they are kept, who can read them, and that nothing prints or leaks them. */
class CredentialsTest {
    @TempDir Path root;

    static final String KEY = "sk-or-v1-0123456789abcdef0123456789abcdef";

    boolean posix() {
        try {
            return Files.getFileStore(root).supportsFileAttributeView("posix");
        } catch (IOException e) {
            return false;
        }
    }

    // ---- the file ----

    @Test
    void aSavedKeyIsReadBackAndTheFileIsReadableByItsOwnerOnly() throws Exception {
        Path file = root.resolve("global/credentials.json");
        var store = new FileCredentialStore(file);
        store.put("OPENROUTER_API_KEY", "  " + KEY + "\n");
        assertEquals(KEY, store.get("OPENROUTER_API_KEY").orElseThrow(), "surrounding whitespace is dropped");
        assertEquals(KEY, new FileCredentialStore(file).get("OPENROUTER_API_KEY").orElseThrow(), "it survives a restart");
        if (posix()) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)), "owner only");
            try (var left = Files.list(file.getParent())) {
                assertEquals(List.of("credentials.json"), left.map(p -> p.getFileName().toString()).toList(), "no temporary file is left behind");
            }
        }
    }

    @Test
    void replacingAndRemovingKeepsTheOtherKeysAndTheOwnerOnlyMode() throws Exception {
        Path file = root.resolve("credentials.json");
        var store = new FileCredentialStore(file);
        store.put("OPENROUTER_API_KEY", KEY);
        store.put("DEEPSEEK_API_KEY", "ds-0123456789abcdef");
        store.put("OPENROUTER_API_KEY", KEY + "x");
        assertEquals(KEY + "x", store.get("OPENROUTER_API_KEY").orElseThrow());
        assertTrue(store.remove("DEEPSEEK_API_KEY"));
        assertFalse(store.remove("DEEPSEEK_API_KEY"), "nothing left to remove");
        assertEquals(java.util.Set.of("OPENROUTER_API_KEY"), new FileCredentialStore(file).variables());
        if (posix()) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        }
    }

    @Test
    void thingsThatAreNotKeysOrVariableNamesAreRefused() {
        var store = new FileCredentialStore(root.resolve("credentials.json"));
        assertThrows(IllegalArgumentException.class, () -> store.put("OPENROUTER_API_KEY", ""));
        assertThrows(IllegalArgumentException.class, () -> store.put("OPENROUTER_API_KEY", "two words"));
        assertThrows(IllegalArgumentException.class, () -> store.put("OPENROUTER_API_KEY", "line\nbreak"));
        assertThrows(IllegalArgumentException.class, () -> store.put("OPENROUTER_API_KEY", "x".repeat(5000)));
        assertThrows(IllegalArgumentException.class, () -> store.put("lower_case", "abc123456"));
        assertThrows(IllegalArgumentException.class, () -> store.put("HAS SPACE", "abc123456"));
        assertFalse(Files.exists(root.resolve("credentials.json")), "a refused key creates no file");
    }

    @Test
    void aDamagedFileIsNeverOverwrittenSoNoKeyIsLostByAccident() throws Exception {
        Path file = root.resolve("credentials.json");
        Files.writeString(file, "{ this is not json");
        var store = new FileCredentialStore(file);
        assertTrue(store.get("OPENROUTER_API_KEY").isEmpty());
        IOException e = assertThrows(IOException.class, () -> store.put("OPENROUTER_API_KEY", KEY));
        assertTrue(e.getMessage().contains("will not overwrite"), e.getMessage());
        assertEquals("{ this is not json", Files.readString(file), "the file is exactly as it was");
    }

    // ---- who reads it ----

    @Test
    void aSetEnvironmentVariableAlwaysWinsOverASavedKeyAndABlankOneDoesNot() throws Exception {
        Path home = Files.createDirectories(root.resolve("home"));
        var out = new PrintStream(new ByteArrayOutputStream());
        var base = Map.of("OPENROUTER_API_KEY", "from-the-environment-123", "DEEPSEEK_API_KEY", "   ");
        var ctx = new CliContext(root, home, base, out, out, new BufferedReader(new StringReader("")), false, (r, s) -> null, (a, b, c) -> { });
        ctx.credentials.put("OPENROUTER_API_KEY", KEY);
        ctx.credentials.put("DEEPSEEK_API_KEY", "ds-0123456789abcdef");
        ctx.credentials.put("GROQ_API_KEY", "gsk-0123456789abcdef");
        assertEquals("from-the-environment-123", ctx.env.get("OPENROUTER_API_KEY"));
        assertEquals("ds-0123456789abcdef", ctx.env.get("DEEPSEEK_API_KEY"), "a blank variable does not hide a saved key");
        assertEquals("gsk-0123456789abcdef", ctx.env.get("GROQ_API_KEY"));
        assertTrue(ctx.env.containsKey("GROQ_API_KEY"));
        assertEquals(null, ctx.env.get("MISTRAL_API_KEY"));
        assertEquals("from-the-environment-123", ctx.processEnv.get("OPENROUTER_API_KEY"), "the raw environment is unchanged");
        assertFalse(ctx.processEnv.containsKey("GROQ_API_KEY"));
    }

    // ---- agents ----

    Agent reader() {
        return new Agent("ana", "dev", "", Set.of("filesystem.read", "filesystem.write", "search"),
                new Permissions(List.of("**"), List.of("**"), List.of(), Duration.ofSeconds(5)));
    }

    /** The workspace is the user's home, as when BuildCLI is started from ~: BuildCLI's own folder is inside it. */
    @Test
    void anAgentCannotReadWriteListOrSearchBuildclisOwnFolderEvenWhenItIsInsideTheWorkspace() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("home"));
        Path own = Files.createDirectories(workspace.resolve(".buildcli"));
        Files.writeString(own.resolve("credentials.json"), "{\"schema\":1,\"keys\":{\"OPENROUTER_API_KEY\":\"" + KEY + "\"}}");
        Files.writeString(workspace.resolve("notes.txt"), "hello " + KEY.substring(0, 12));
        var ctx = new ToolContext(workspace, request -> true).protect(List.of(own));

        var read = new ToolCall("1", "read_file", Map.of("path", ".buildcli/credentials.json"));
        var e = assertThrows(IllegalArgumentException.class, () -> new ReadFileTool().execute(ctx, reader(), read));
        assertTrue(e.getMessage().contains("BuildCLI's own folder"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new WriteFileTool().execute(ctx, reader(),
                new ToolCall("2", "write_file", Map.of("path", ".buildcli/trust.json", "content", "{}"))));
        assertFalse(Files.exists(own.resolve("trust.json")), "nothing was written");
        assertThrows(IllegalArgumentException.class, () -> new ListFilesTool().execute(ctx, reader(),
                new ToolCall("3", "list_files", Map.of("path", ".buildcli"))));

        String listing = new ListFilesTool().execute(ctx, reader(), new ToolCall("4", "list_files", Map.of("path", ".")));
        assertTrue(listing.contains("notes.txt") && !listing.contains(".buildcli"), "the folder is not even listed:\n" + listing);
        String found = new SearchTool().execute(ctx, reader(), new ToolCall("5", "search", Map.of("pattern", "sk-or-v1")));
        assertFalse(found.contains("credentials.json"), "search does not look inside it:\n" + found);
        assertTrue(new SearchTool().execute(ctx, reader(), new ToolCall("6", "search", Map.of("pattern", "hello"))).contains("notes.txt"),
                "the rest of the workspace is still searched");
    }

    @Test
    void aSymlinkIntoBuildclisFolderDoesNotOpenIt() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("work"));
        Path own = Files.createDirectories(root.resolve("elsewhere/.buildcli"));
        Files.writeString(own.resolve("credentials.json"), "secret");
        try {
            Files.createSymbolicLink(workspace.resolve("link"), own);
        } catch (UnsupportedOperationException | IOException e) {
            return; // this system cannot make symlinks here; the other tests cover the rule
        }
        var ctx = new ToolContext(workspace, request -> true).protect(List.of(own));
        assertThrows(IllegalArgumentException.class, () -> ctx.resolve("link/credentials.json"));
    }

    @Test
    void ifAnAgentIsEverShownTheFileItsKeysAreMaskedInTheOutput() {
        String content = "{\"schema\":1,\"keys\":{\"OPENROUTER_API_KEY\":\"" + KEY + "\"}}";
        String shown = Redactor.redact(content);
        assertFalse(shown.contains(KEY), shown);
        assertFalse(shown.contains("0123456789abcdef"), shown);
    }

    // ---- the command line ----

    record Out(int code, String out, String err) {}

    Out cli(String stdin, Map<String, String> env, Path home, String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var ctx = new CliContext(root.resolve("project"), home, env, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8), new BufferedReader(new StringReader(stdin)), false, (r, s) -> null,
                (a, b, c) -> { });
        int code = BuildCli.run(args, ctx);
        return new Out(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void providerLoginSavesFromStandardInputWithoutEverPrintingTheKey() throws Exception {
        Path home = Files.createDirectories(root.resolve("home"));
        Files.createDirectories(root.resolve("project"));
        Out login = cli(KEY + "\n", Map.of(), home, "provider", "login", "openrouter", "--stdin");
        assertEquals(0, login.code, login.err);
        assertFalse((login.out + login.err).contains(KEY), "the key is never echoed");
        assertTrue(login.out.contains("Saved the key for openrouter (OPENROUTER_API_KEY)"), login.out);
        assertEquals(KEY, new FileCredentialStore(home.resolve(".buildcli/credentials.json")).get("OPENROUTER_API_KEY").orElseThrow());

        Out list = cli("", Map.of(), home, "provider", "list");
        assertTrue(list.out.contains("OPENROUTER_API_KEY (saved)"), list.out);
        assertFalse(list.out.contains(KEY));
        Out config = cli("", Map.of(), home, "config");
        assertTrue(config.out.contains("OPENROUTER_API_KEY: saved by you (not shown)") || config.out.contains("saved by you (not shown)"), config.out);
        assertFalse(config.out.contains(KEY));

        Out withEnv = cli("", Map.of("OPENROUTER_API_KEY", "other-0123456789"), home, "provider", "list");
        assertTrue(withEnv.out.contains("OPENROUTER_API_KEY (environment)"), "a set variable is what counts:\n" + withEnv.out);

        Out logout = cli("", Map.of(), home, "provider", "logout", "openrouter");
        assertEquals(0, logout.code, logout.err);
        assertFalse(Files.readString(home.resolve(".buildcli/credentials.json")).contains(KEY), "gone from the file");
        assertEquals(1, cli("", Map.of(), home, "provider", "logout", "openrouter").code, "nothing left to forget");
    }

    @Test
    void loginRefusesUnknownProvidersProvidersWithoutKeysAndBadInputAndAsksNothingWithoutATerminal() throws Exception {
        Path home = Files.createDirectories(root.resolve("home"));
        Files.createDirectories(root.resolve("project"));
        assertEquals(2, cli(KEY, Map.of(), home, "provider", "login", "nope", "--stdin").code);
        assertEquals(2, cli(KEY, Map.of(), home, "provider", "login", "ollama", "--stdin").code, "ollama needs no key");
        assertEquals(2, cli("two words\n", Map.of(), home, "provider", "login", "openrouter", "--stdin").code);
        assertEquals(2, cli("\n", Map.of(), home, "provider", "login", "openrouter", "--stdin").code);
        Out noTerminal = cli("", Map.of(), home, "provider", "login", "openrouter");
        assertEquals(2, noTerminal.code);
        assertTrue(noTerminal.err.contains("--stdin"), noTerminal.err);
        assertFalse(Files.exists(home.resolve(".buildcli/credentials.json")), "nothing was saved by any of those");
    }

    @Test
    void thatKeyIsNotPassedToCommandsAnAgentRuns() {
        var env = new TreeMapFriendly().with("OPENROUTER_API_KEY", KEY).with("PATH", "/usr/bin");
        assertFalse(dev.buildcli.application.tools.SafeEnvironment.filter(env.map).containsKey("OPENROUTER_API_KEY"));
    }

    /** Tiny builder so the test reads top to bottom. */
    static final class TreeMapFriendly {
        final Map<String, String> map = new java.util.HashMap<>();

        TreeMapFriendly with(String k, String v) {
            map.put(k, v);
            return this;
        }
    }
}
