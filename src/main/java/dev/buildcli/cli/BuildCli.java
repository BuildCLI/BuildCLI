package dev.buildcli.cli;

import dev.buildcli.domain.ModelRef;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IFactory;

/** The {@code buildcli} command. Without arguments on a terminal it opens the TUI; otherwise it prints the help. */
// INHERIT gives every subcommand --help and --version too, e.g. "buildcli agent create --help"
@Command(name = "buildcli", mixinStandardHelpOptions = true, scope = CommandLine.ScopeType.INHERIT, versionProvider = BuildCli.Version.class,
        description = "Your local AI engineering team: agents you chat with, that work on your project with the permissions you give them.",
        subcommands = {InitCommand.class, AgentCommand.class, RunCommand.class, RunsCommand.class,
                TaskCommand.class, UsageCommand.class, DoctorCommand.class, ConfigCommand.class, ProviderCommand.class})
public final class BuildCli implements Callable<Integer> {
    /** Set by picocli; used to open the TUI or print help when no subcommand is given. */
    @CommandLine.Spec CommandLine.Model.CommandSpec spec;
    private final CliContext ctx;

    public BuildCli(CliContext ctx) {
        this.ctx = ctx;
    }

    public static void main(String[] args) {
        System.exit(run(args, CliContext.system()));
    }

    /** Runs the CLI against a context and returns the exit code (0 ok, 1 the work failed, 2 usage or configuration error). */
    public static int run(String[] args, CliContext ctx) {
        CommandLine cmd = new CommandLine(new BuildCli(ctx), factory(ctx));
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setOut(new java.io.PrintWriter(ctx.out, true));
        cmd.setErr(new java.io.PrintWriter(ctx.err, true));
        cmd.setExecutionExceptionHandler((e, c, parseResult) -> {
            ctx.err.println("error: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
            if (ctx.env.containsKey("BUILDCLI_DEBUG")) {
                e.printStackTrace(ctx.err);
            }
            return 1;
        });
        return cmd.execute(args);
    }

    /** Lets commands take the context in their constructor. */
    static IFactory factory(CliContext ctx) {
        return new IFactory() {
            @Override
            public <K> K create(Class<K> cls) throws Exception {
                try {
                    return cls.getDeclaredConstructor(CliContext.class).newInstance(ctx);
                } catch (NoSuchMethodException e) {
                    return CommandLine.defaultFactory().create(cls);
                }
            }
        };
    }

    @Override
    public Integer call() {
        if (ctx.terminal) {
            return spec.commandLine().execute("run");
        }
        spec.commandLine().usage(ctx.out);
        return 0;
    }

    /** Parses {@code provider:model}, for example {@code ollama:qwen2.5:7b} (the model may itself contain colons). */
    static ModelRef parseModel(String value) {
        int i = value.indexOf(':');
        if (i <= 0 || i == value.length() - 1) {
            throw new IllegalArgumentException("model must be provider:model, for example ollama:qwen2.5:7b (got '" + value + "')");
        }
        return new ModelRef(value.substring(0, i), value.substring(i + 1));
    }

    static final class Version implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String v = BuildCli.class.getPackage().getImplementationVersion();
            return new String[] {"buildcli " + (v == null ? "dev" : v)};
        }
    }
}
