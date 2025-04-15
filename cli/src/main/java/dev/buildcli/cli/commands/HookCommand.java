package dev.buildcli.cli.commands;

import dev.buildcli.cli.commands.hook.HookAddCommand;
import dev.buildcli.cli.commands.hook.HookListCommand;
import dev.buildcli.cli.commands.hook.HookRemoveCommand;
import picocli.CommandLine.Command;

@Command(name="hook",
description = "Manage hooks. Hooks are commands that are executed before or after a specific command.",
subcommands = {HookAddCommand.class, HookRemoveCommand.class, HookListCommand.class})
public class HookCommand{
}
