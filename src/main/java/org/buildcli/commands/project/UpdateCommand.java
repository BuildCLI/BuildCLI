package org.buildcli.commands.project;

import org.buildcli.commands.project.update.DependencyCommand;
import org.buildcli.commands.project.update.JDKCommand;
import org.buildcli.commands.project.update.VersionCommand;
import picocli.CommandLine.Command;

@Command(name = "update", aliases = {"up"}, description = "Updates project versions and dependencies.", mixinStandardHelpOptions = true,
    subcommands = {VersionCommand.class, DependencyCommand.class, JDKCommand.class}
)
public class UpdateCommand {
}
