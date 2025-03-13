package dev.buildcli.core.actions.changelog;

import dev.buildcli.core.domain.git.GitOperations;
import picocli.CommandLine.Model.CommandSpec;

import java.util.*;

public class ChangelogManager {

    private GitOperations gitOperations;
    private CommandSpec spec;
    private String repositoryDir;

    public ChangelogManager(CommandSpec spec, String repositoryDir) {
        this.gitOperations = new GitOperations();
        this.spec = spec;
        this.repositoryDir = repositoryDir;
    }

    public void generateChangelog(String version, String outputFile,
                                         String format, List<String> includeTypes) {
        try {
            var releaseVersion = (version != null) ? version :  gitOperations.getLatestGitTag().orElse("Unreleased");
            var outputFileName = ChangelogFileUtils.formatOutputFile(outputFile, format);

            String contents = ChangelogGenerator.generateChangeLogContents(gitOperations, releaseVersion, repositoryDir, includeTypes, format);
            ChangelogFileUtils.writeToFile(contents, outputFileName);

            spec.commandLine().getOut().println("Changelog generated successfully.");
        } catch (Exception e) {
            spec.commandLine().getOut().println("Failed to generate changelog.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
