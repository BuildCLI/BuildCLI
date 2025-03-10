package dev.buildcli.core.utils;

import dev.buildcli.core.utils.formatter.Formatter;
import dev.buildcli.core.utils.formatter.FormatterFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.buildcli.core.constants.ChangelogConstants.ORDERED_SECTIONS;

public class ChangelogManager {

    /**
     * Matches commit messages in the format: {@code <type>(<scope>): <subject>}.
     *
     * <ul>
     *   <li><strong>type</strong>: A single word identifying the type of change (e.g., feat, fix, docs).</li>
     *   <li><strong>scope</strong>: An optional scope in parentheses (e.g., core, database).</li>
     *   <li><strong>subject</strong>: The subject of the commit message.</li>
     * </ul>
     */
    private static final Pattern COMMIT_PATTERN = Pattern.compile("^(\\w+)(?:\\(([^)]+)\\))?\\s*:\\s*(.*)$");

    private static final Map<String, String> TYPE_TO_KEEP_SECTION = Map.ofEntries(
            Map.entry("feat", "Added"),
            Map.entry("docs", "Added"),
            Map.entry("fix", "Fixed"),
            Map.entry("refactor", "Changed"),
            Map.entry("perf", "Changed"),
            Map.entry("chore", "Changed"),
            Map.entry("style", "Changed"),
            Map.entry("ci", "Changed"),
            Map.entry("build", "Changed"),
            Map.entry("test", "Changed"),
            Map.entry("deprecated", "Deprecated"),
            Map.entry("remove", "Removed"),
            Map.entry("revert", "Removed"),
            Map.entry("security", "Security")
    );

    private static File repositoryDir = new File(".");

    @Spec
    private static CommandSpec spec;

    public static void generateChangelog(String version, String outputFile,
                                         String format, List<String> includeTypes) {
        try {
            var releaseVersion = (version != null) ? version :  getLatestGitTag().orElse("Unreleased");
            var outputFileName = formatOutputFile(outputFile, format);

            generateChangeLogContents(releaseVersion, outputFileName, includeTypes, format);
            spec.commandLine().getOut().println("Changelog generated successfully.");
        } catch (Exception e) {
            spec.commandLine().getOut().println("Failed to generate changelog.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected static String formatOutputFile(String fileName, String format) {
        if (fileName == null | fileName.isBlank()) {
            return "CHANGELOG" + FileTypes.fromExtension(format);
        }
        String outputFileName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String extension = FileTypes.fromExtension(format);
        return Path.of(outputFileName + extension).toString();
    }

    protected static void generateChangeLogContents(String releaseVersion, String outputFile,
                                                    List<String> includeTypes, String format) throws IOException, GitAPIException {
        try(Git git = Git.open(repositoryDir)){

            Map<String, Map<String, List<String>>> versionedData = new LinkedHashMap<>();
            versionedData.put(releaseVersion, new LinkedHashMap<>());

            for(String section : ORDERED_SECTIONS) {
                versionedData.get(releaseVersion).put(section, new ArrayList<>());
            }

            versionedData.get(releaseVersion).put("Other", new ArrayList<>());

            Iterable<RevCommit> commits = git.log().call();
            for (RevCommit commit : commits) {
                String fullMessage = commit.getFullMessage().trim();
                Matcher matcher    = COMMIT_PATTERN.matcher(fullMessage);

                if (matcher.find()) {
                    String type = matcher.group(1).toLowerCase();
                    String scope = matcher.group(2);
                    String message = matcher.group(3).trim();

                    if(!includeTypes.contains("all") && !includeTypes.contains(type)) {
                        continue;
                    }

                    String formattedMessage = (scope != null && !scope.isEmpty())
                            ? String.format("%s(%s): %s", type, scope, message)
                            : String.format("%s : %s", type, message);

                    String keepSection = TYPE_TO_KEEP_SECTION.getOrDefault(type, "Other");
                    versionedData.get(releaseVersion).get(keepSection).add(formattedMessage);
                }
            }

            String content = generateOutput(versionedData, format);
            writeToFile(content, outputFile);

        }
    }

    protected static Optional<String> getLatestGitTag() throws IOException {

        try (Git git = Git.open(repositoryDir)) {
            List<Ref> taglist = git.tagList().call();
            if (!taglist.isEmpty()) {
                return taglist.stream()
                        .map(ref -> ref.getName().replace("refs/tags/", ""))
                        .max(Comparator.naturalOrder());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    protected static String generateOutput(Map<String, Map<String, List<String>>> data, String format) {
        Formatter formatter = FormatterFactory.getFormatter(format.toLowerCase());

        return formatter.generate(data);
    }

    protected static void writeToFile(String content, String outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)){
            writer.write(content);
        }
    }
}
