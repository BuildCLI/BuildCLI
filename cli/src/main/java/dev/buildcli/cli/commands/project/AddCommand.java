package dev.buildcli.cli.commands.project;

import dev.buildcli.cli.commands.project.add.DependencyCommand;
import dev.buildcli.cli.commands.project.add.DockerfileCommand;
import dev.buildcli.cli.commands.project.add.PipelineCommand;
import dev.buildcli.cli.commands.project.add.ProfileCommand;
import picocli.CommandLine.Command;

@Command(name = "add", aliases = {"a"}, description = "Adds a new item to the project. This command "
        + "allows adding dependencies, pipelines, profiles, and Dockerfiles.",
        subcommands = {DependencyCommand.class, PipelineCommand.class, ProfileCommand.class, DockerfileCommand.class},
        mixinStandardHelpOptions = true
)
public class AddCommand {

}
