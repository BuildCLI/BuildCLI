package dev.buildcli.cli.commands.hook;

import dev.buildcli.cli.BuildCLI;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.hooks.HookManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name="list",
         description = """
                        List all hooks configured for commands.
                        Usage: hook list
                        This will display all registered hooks and their associated commands.
                        """)
public class HookListCommand implements BuildCLICommand {

    @Override
    public void run(){
     new HookManager(new CommandLine(new BuildCLI())).listHooks();
    }
}
