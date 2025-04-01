package dev.buildcli.cli.commands.run;


import picocli.CommandLine.Command;

@Command(
        name = "orchestration",
        aliases = "oc",
        description = "Manage container orchestration using Docker Compose.",
        mixinStandardHelpOptions = true,
        subcommands = {
                OrchestrationUpCommand.class,
                OrchestrationDownCommand.class
        }
)
public class OrchestrationCommand {}