package dev.buildcli.core.actions.changelog;

import dev.buildcli.core.domain.git.GitOperations;
import dev.buildcli.core.utils.formatter.Formatter;
import dev.buildcli.core.utils.formatter.FormatterFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static dev.buildcli.core.constants.ChangelogConstants.ORDERED_SECTIONS;

public class ChangelogGenerator {

    public static String generateChangeLogContents(GitOperations gitOperations, String releaseVersion, String repositoryDir,
                                                   List<String> includeTypes, String format) throws IOException, GitAPIException {

        try (Git git = gitOperations.openGitRepository(repositoryDir)) {

            Map<String, Map<String, List<String>>> versionedData = new LinkedHashMap<>();
            versionedData.put(releaseVersion, new LinkedHashMap<>());

            for (String section : ORDERED_SECTIONS) {
                versionedData.get(releaseVersion).put(section, new ArrayList<>());
            }

            versionedData.get(releaseVersion).put("Other", new ArrayList<>());

            Iterable<RevCommit> commits = gitOperations.gitLog("");
            for (RevCommit commit : commits) {
                String fullMessage = commit.getFullMessage().trim();
                Matcher matcher = ChangelogConstants.COMMIT_PATTERN.matcher(fullMessage);

                if (matcher.find()) {
                    String type = matcher.group(1).toLowerCase();
                    String scope = matcher.group(2);
                    String message = matcher.group(3).trim();

                    if (!includeTypes.contains("all") && !includeTypes.contains(type)) {
                        continue;
                    }

                    String formattedMessage = (scope != null && !scope.isEmpty())
                            ? String.format("%s(%s): %s", type, scope, message)
                            : String.format("%s : %s", type, message);

                    String keepSection = ChangelogConstants.TYPE_TO_KEEP_SECTION.getOrDefault(type, "Other");
                    versionedData.get(releaseVersion).get(keepSection).add(formattedMessage);
                }
            }

            return generateOutput(versionedData, format);
        }
    }

    protected static String generateOutput(Map<String, Map<String, List<String>>> data, String format) {
        Formatter formatter = FormatterFactory.getFormatter(format.toLowerCase());

        return formatter.generate(data);
    }
}
