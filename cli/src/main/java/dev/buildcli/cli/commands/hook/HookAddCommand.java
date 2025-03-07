package dev.buildcli.cli.commands.hook;

import dev.buildcli.cli.BuildCLI;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.hooks.Hook;
import dev.buildcli.hooks.HookManager;
import dev.buildcli.hooks.HookPhase;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Command(
        name = "add",
        description = "Add a hook to a command. Usage: hook add [before/after] \"[command]\", \"[hookCommand]\"" +
                "Example: 'add before \"project build\" \"project clean\"' . This will execute 'project clean' before 'project build'"
)
public class HookAddCommand implements BuildCLICommand {

    @Parameters(index = "0", description = "When your hook will be executed (before/after)")
    private String phase;

    @Parameters(index = "1", arity ="1..*", description = "Main command for attach the hook")
    private String command;

    @Parameters(index = "2", arity = "1..*", description = "HookCommand to be executed")
    private String hookCommand;

    @Override
    public void run() {
        HookManager hookManager = new HookManager(new CommandLine(new BuildCLI()));

        HookPhase hookPhase = HookPhase.valueOf(phase.toUpperCase());
        Hook hook = new Hook(command, hookPhase, hookCommand);

        hookManager.registerHook(hook);
    }
}
