package dev.buildcli.cli.commands.project;

import dev.buildcli.cli.commands.project.add.*;
import picocli.CommandLine.Command;

@Command(name = "add", aliases = {"a"}, description = "Adds a new item to the project. This command "
        + "allows adding dependencies, pipelines, profiles, and Dockerfiles.",
        subcommands = {DependencyCommand.class, PipelineCommand.class, ProfileCommand.class, DockerfileCommand.class,
                DockerComposeCommand.class},
        mixinStandardHelpOptions = true
)
public class AddCommand {

}
