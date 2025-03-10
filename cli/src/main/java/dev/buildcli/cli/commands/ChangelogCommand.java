package dev.buildcli.cli.commands;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.ChangelogManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.*;


@Command(
        name = "changelog",
        aliases = {"cl"},
        description = "Prints the changelog for BuildCLI.",
        mixinStandardHelpOptions = true)
public class ChangelogCommand implements BuildCLICommand {

    @Option(
            names = {"--version", "-v"},
            description = "Release version to label the generated changelog (e.g. 1.2.3). If not specified, use the latest Git tag or 'Unreleased'."
    )
    private String version;

    @Option(
            names = {"--format", "-f"},
            description = "Output format. Supported: markdown, html and json. (default: markdown)",
            defaultValue = "markdown"
    )
    private String format;

    @Option(
            names = {"--output", "-o"},
            description  = "The output file to write the changelog to. " +
                    "If not specified, will use 'CHANGELOG.<format>'.",
            defaultValue = "CHANGELOG"
    )
    private String outputFile;

    @Option(
            names = {"--include", "-i"},
            description  = "Comma-separated list of commit types to include (default: all)",
            split        = ",",
            defaultValue = "all"
    )
    private List<String> includeTypes = new ArrayList<>();

    @Override
    public void run() {
        ChangelogManager.generateChangelog(version, outputFile, format, includeTypes);
    }


}


