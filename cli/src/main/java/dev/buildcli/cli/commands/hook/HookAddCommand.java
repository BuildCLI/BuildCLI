package dev.buildcli.cli.commands.hook;

import dev.buildcli.cli.BuildCLI;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.hooks.Hook;
import dev.buildcli.hooks.HookManager;
import dev.buildcli.hooks.phase.HookPhase;
import dev.buildcli.hooks.phase.HookPhaseConverter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Command(
        name = "add",
        description ="""
                    Add a hook to a command.
                    Usage: hook add [before/after] "[command]" "[hookCommand]"
                    Example: add b "project build" "project clean"
                    This will execute 'project clean' before 'project build'.
                    """
)
public class HookAddCommand implements BuildCLICommand {

    @Parameters(index="0", description =  "Defines whether the hook should be executed before or after the command.\n" +
            "Accepted values: 'before', 'after' (or shorthand 'b' and 'a').", converter = HookPhaseConverter.class)
    public HookPhase phase;

    @Parameters(index = "1", description = "Main command for attach the hook")
    private String command;

    @Parameters(index = "2", description = "HookCommand to be executed")
    private String hookCommand;

    @Override
    public void run() {
        HookManager hookManager = new HookManager(new CommandLine(new BuildCLI()));

        Hook hook = new Hook(command, phase, hookCommand);

        hookManager.registerHook(hook);
    }
}
