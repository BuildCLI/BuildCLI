package dev.buildcli.cli;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ConfigRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "agent", description = "List, show and create agents", subcommands = {AgentCommand.ListCmd.class,
        AgentCommand.ShowCmd.class, AgentCommand.CreateCmd.class})
final class AgentCommand implements Callable<Integer> {
    private final CliContext ctx;

    AgentCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer call() {
        ctx.out.println("Use one of: agent list | agent show <name> | agent create <name>");
        return 2;
    }

    @Command(name = "list", description = "List the available agents")
    static final class ListCmd implements Callable<Integer> {
        private final CliContext ctx;

        ListCmd(CliContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Integer call() {
            ConfigRepository config = ctx.loadConfig();
            if (config == null) {
                return 2;
            }
            if (config.agents().isEmpty()) {
                ctx.out.println("No agents yet. Run 'buildcli init' for sample agents, or 'buildcli agent create <name>'.");
                return 0;
            }
            Tables.print(ctx.out, List.of("NAME", "ROLE", "ORIGIN", "CAPABILITIES"), config.agents().stream()
                    .map(a -> List.of(a.name(), a.role(), a.origin().name().toLowerCase(), String.join(",", a.capabilities()))).toList());
            return 0;
        }
    }

    @Command(name = "show", description = "Show one agent in full")
    static final class ShowCmd implements Callable<Integer> {
        private final CliContext ctx;

        @Parameters(index = "0", description = "Agent name")
        String name;

        ShowCmd(CliContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Integer call() {
            ConfigRepository config = ctx.loadConfig();
            if (config == null) {
                return 2;
            }
            Agent a = config.agent(name).orElse(null);
            if (a == null) {
                ctx.err.println("error: no agent named '" + name + "'. Available: " + config.agents().stream().map(Agent::name).toList());
                return 2;
            }
            ctx.out.println(a.name() + "  (" + a.role() + ", " + a.origin().name().toLowerCase() + ")");
            ctx.out.println("  defined in:   " + a.source());
            ctx.out.println("  capabilities: " + a.capabilities());
            ctx.out.println("  may read:     " + a.permissions().readGlobs());
            ctx.out.println("  may write:    " + a.permissions().writeGlobs());
            ctx.out.println("  may run:      " + a.permissions().commandAllow() + " (timeout " + a.permissions().commandTimeout() + ")");
            ctx.out.println("  instructions:");
            a.instructions().lines().forEach(l -> ctx.out.println("    " + l));
            return 0;
        }
    }

    @Command(name = "create", description = "Create an agent file from a template")
    static final class CreateCmd implements Callable<Integer> {
        private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_-]*");
        private final CliContext ctx;

        @Parameters(index = "0", description = "Agent name (lowercase letters, digits, - and _)")
        String name;

        @Option(names = "--role", description = "Role, e.g. writer or developer (default: ${DEFAULT-VALUE})", defaultValue = "assistant")
        String role;

        @Option(names = "--capabilities", split = ",", description = "Comma-separated capabilities (default: filesystem.read,search)")
        List<String> capabilities;

        @Option(names = "--global", description = "Create it in ~/.buildcli instead of the project")
        boolean global;

        CreateCmd(CliContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Integer call() throws IOException {
            if (!NAME.matcher(name).matches()) {
                ctx.err.println("error: the name must match [a-z][a-z0-9_-]*");
                return 2;
            }
            List<String> caps = capabilities == null ? List.of(Capability.FILESYSTEM_READ, Capability.SEARCH) : capabilities;
            List<String> unknown = caps.stream().filter(c -> !Capability.KNOWN.contains(c)).toList();
            if (!unknown.isEmpty()) {
                ctx.err.println("error: unknown capabilities " + unknown + "; known: " + new TreeSet<>(Capability.KNOWN));
                return 2;
            }
            Path file = (global ? ctx.globalDir() : ctx.cwd.resolve(".buildcli")).resolve("agents").resolve(name + ".md");
            if (Files.exists(file)) {
                ctx.err.println("error: " + file + " already exists");
                return 2;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, template(name, role, caps, ""), StandardCharsets.UTF_8);
            ctx.out.println("created  " + file);
            ctx.out.println("Edit its instructions and permissions there, then run 'buildcli' and chat with " + name + ".");
            if (!global) {
                ctx.out.println("Note: BuildCLI asks you to approve a new or changed project agent before it first runs.");
            }
            return 0;
        }

        /** The agent file; {@code how} is the instructions, or a placeholder telling the user what to write there. */
        static String template(String name, String role, List<String> caps, String how) {
            String body = how == null || how.isBlank()
                    ? "Describe how this agent should behave: its responsibility, what it must not do, and how it reports back." : how.strip();
            return """
                    ---
                    schema: 1
                    name: %s
                    role: %s
                    capabilities: [%s]
                    permissions:
                      filesystem:
                        read: ["**"]
                    ---
                    %s
                    """.formatted(name, role, String.join(", ", caps), body);
        }
    }

}
