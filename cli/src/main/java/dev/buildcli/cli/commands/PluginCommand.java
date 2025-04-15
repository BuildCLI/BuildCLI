package dev.buildcli.cli.commands;

import dev.buildcli.cli.commands.plugin.AddCommand;
import dev.buildcli.cli.commands.plugin.InitCommand;
import dev.buildcli.cli.commands.plugin.ListCommand;
import dev.buildcli.cli.commands.plugin.RmCommand;
import picocli.CommandLine.Command;

@Command(name = "plugin", aliases = {"plg"}, description = "Manage plugins easy, add, create, list and remove",
    mixinStandardHelpOptions = true, subcommands = {AddCommand.class, InitCommand.class, ListCommand.class, RmCommand.class}
)
public class PluginCommand {
}
