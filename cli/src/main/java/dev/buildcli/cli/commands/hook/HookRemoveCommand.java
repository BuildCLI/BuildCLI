package dev.buildcli.cli.commands.hook;

import dev.buildcli.cli.BuildCLI;
import dev.buildcli.core.domain.BuildCLICommand;

import dev.buildcli.hooks.HookManager;
import dev.buildcli.hooks.phase.HookPhase;
import dev.buildcli.hooks.phase.HookPhaseConverter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
        name = "remove",
        description = """
                Remove a hook from a command.
                Usage: hook remove [before/after] "[command]"
                Example: remove b "project build"
                This will remove any command that run before 'project build'.
                """
)
public class HookRemoveCommand implements BuildCLICommand {

    @Parameters(index = "0",converter = HookPhaseConverter.class,
            description =  "Defines whether the hook should be removed before or after the command.\n" +
            "Accepted values: 'before', 'after' (or shorthand 'b' and 'a').")
    public HookPhase phase;

    @Parameters(index = "1", description = "Command to remove the hook")
    private String command;

    @Override
    public void run() {
        HookManager hookManager = new HookManager(new CommandLine(new BuildCLI()));

        hookManager.removeHook(command, phase);
    }
}
