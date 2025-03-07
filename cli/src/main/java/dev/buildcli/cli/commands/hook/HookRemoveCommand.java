package dev.buildcli.cli.commands.hook;

import dev.buildcli.cli.BuildCLI;
import dev.buildcli.core.domain.BuildCLICommand;

import dev.buildcli.hooks.HookManager;
import dev.buildcli.hooks.HookPhase;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
        name = "remove",
        description = "Remove a hook from a command. Usage: hook remove [before/after] \"[command]\""
                + "Example: 'remove before \"project build\"' . This will remove any command before from 'project build'"
)
public class HookRemoveCommand implements BuildCLICommand {

    @Parameters(index = "0", description = "Hook phase [before/after]")
    private String phase;

    @Parameters(index = "1",arity ="1..*", description = "Command to remove the hook")
    private String command;

    @Override
    public void run() {
        HookManager hookManager = new HookManager(new CommandLine(new BuildCLI()));

        HookPhase hookPhase = HookPhase.valueOf(phase.toUpperCase());
        hookManager.removeHook(command, hookPhase);
    }
}
