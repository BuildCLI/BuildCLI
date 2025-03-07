package dev.buildcli.cli.commands;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.FileTypes;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
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

@Command(name = "changelog", aliases = {
    "cl" }, description = "Prints the changelog for BuildCLI.", mixinStandardHelpOptions = true)
public class ChangelogCommand implements BuildCLICommand {

  @Spec
  private CommandSpec spec;

  @Option(names = { "--version",
      "-v" }, description = "Release version to label the generated changelog (e.g. 1.2.3). If not specified, use the latest Git tag or 'Unreleased'.")
  private String version;

  @Option(names = { "--format",
      "-f" }, description = "Output format. Supported: markdown, html and json. (default: markdown)", defaultValue = "markdown")
  private String format;

  @Option(names = { "--output", "-o" }, description = "The output file to write the changelog to. " +
      "If not specified, will use 'CHANGELOG.<format>'.", defaultValue = "CHANGELOG")
  private String outputFile;

  @Option(names = { "--include",
      "-i" }, description = "Comma-separated list of commit types to include (default: all)", split = ",", defaultValue = "all")
  private List<String> includeTypes = new ArrayList<>();

  /**
   * Matches commit messages in the format: {@code <type>(<scope>): <subject>}.
   *
   * <ul>
   * <li><strong>type</strong>: A single word identifying the type of change
   * (e.g., feat, fix, docs).</li>
   * <li><strong>scope</strong>: An optional scope in parentheses (e.g., core,
   * database).</li>
   * <li><strong>subject</strong>: The subject of the commit message.</li>
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
      Map.entry("security", "Security"));

  private File repositoryDir = new File(".");

  @Override
  public void run() {
    try {

      var releaseVersion = (version != null) ? version : getLatestGitTag().orElse("Unreleased");
      var outputFileName = formatOutputFile(outputFile, format);

      generateChangeLog(releaseVersion, outputFileName, includeTypes);
      spec.commandLine().getOut().println("Changelog generated successfully.");
    } catch (Exception e) {
      spec.commandLine().getOut().println("Failed to generate changelog.");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  protected String formatOutputFile(String fileName, String format) {
    if (fileName == null | fileName.isBlank()) {
      return "CHANGELOG" + FileTypes.fromExtension(format);
    }
    String outputFileName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
    String extension = FileTypes.fromExtension(format);
    return outputFile == null ? "CHANGELOG" + extension : Path.of(outputFileName + extension).toString();
  }

  protected void generateChangeLog(String releaseVersion, String outputFile, List<String> includeTypes)
      throws IOException, GitAPIException {
    try (Git git = Git.open(repositoryDir)) {

      Map<String, Map<String, List<String>>> versionedData = new LinkedHashMap<>();
      versionedData.put(releaseVersion, new LinkedHashMap<>());

      for (String section : ORDERED_SECTIONS) {
        versionedData.get(releaseVersion).put(section, new ArrayList<>());
      }

      versionedData.get(releaseVersion).put("Other", new ArrayList<>());

      Iterable<RevCommit> commits = git.log().call();
      for (RevCommit commit : commits) {
        String fullMessage = commit.getFullMessage().trim();
        Matcher matcher = COMMIT_PATTERN.matcher(fullMessage);

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

          String keepSection = TYPE_TO_KEEP_SECTION.getOrDefault(type, "Other");
          versionedData.get(releaseVersion).get(keepSection).add(formattedMessage);
        }
      }

      String content = generateOutput(versionedData, format);
      writeToFile(content, outputFile);

    }
  }

  protected Optional<String> getLatestGitTag() throws IOException {
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

  protected String generateOutput(Map<String, Map<String, List<String>>> data, String format) {
    return switch (format.toLowerCase()) {
      case "html" -> generateHtml(data);
      case "json" -> generateJson(data);
      default -> generateMarkdown(data);
    };
  }

  private String generateMarkdown(Map<String, Map<String, List<String>>> data) {
    var sb = new StringBuilder();
    sb.append("""
        # Changelog

        All notable changes to this project will be documented in this file.

        The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
        and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

        """);

    data.forEach((version, sections) -> {
      sb.append("## [%s] - %s%n%n".formatted(version, LocalDate.now()));

      ORDERED_SECTIONS.forEach(section -> {
        var commits = sections.get(section);
        if (commits != null && !commits.isEmpty()) {
          sb.append("### %s%n".formatted(section));
          commits.forEach(commitsMsg -> sb.append("- %s%n".formatted(commitsMsg)));
          sb.append("\n");
        }
      });
    });

    return sb.toString();
  }

  private String generateHtml(Map<String, Map<String, List<String>>> data) {
    StringBuilder sb = new StringBuilder();

    sb.append("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Changelog</title>
        </head>
        <body>
            <h1>Changelog</h1>
            <p>All notable changes to this project will be documented in this file.<br>
            The format is based on <a href="https://keepachangelog.com/en/1.0.0/">Keep a Changelog</a>,<br>
            and this project adheres to <a href="https://semver.org/spec/v2.0.0.html">Semantic Versioning</a>.</p>
        """);

    data.forEach((version, sectionMap) -> {
      sb.append("<h2>[%s] - %s</h2>\n".formatted(version, LocalDate.now()));

      ORDERED_SECTIONS.forEach(section -> {
        var commits = sectionMap.get(section);
        if (commits != null && !commits.isEmpty()) {
          sb.append("<h3>%s</h3>\n<ul>\n".formatted(section));
          commits.forEach(commitMsg -> sb.append("  <li>%s</li>\n".formatted(commitMsg)));
          sb.append("</ul>\n");
        }
      });
    });

    sb.append("""
        </body>
        </html>
        """);

    return sb.toString();
  }

  private String generateJson(Map<String, Map<String, List<String>>> data) {
    var sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"changelog\": {\n");
    sb.append("    \"description\": \"All notable changes to this project will be documented in this file." +
        " The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.\",\n");
    sb.append("    \"versions\": [\n");

    var versions = data.entrySet().stream().toList();
    for (int i = 0; i < versions.size(); i++) {
      var entry = versions.get(i);
      var version = entry.getKey();
      var sections = entry.getValue();

      sb.append("      {\n");
      sb.append("        \"version\": \"").append(version).append("\",\n");
      sb.append("        \"date\": \"").append(LocalDate.now()).append("\",\n");
      sb.append("        \"sections\": {\n");

      var nonEmptySections = ORDERED_SECTIONS.stream()
          .filter(section -> sections.containsKey(section) && !sections.get(section).isEmpty())
          .toList();

      for (int j = 0; j < nonEmptySections.size(); j++) {
        var section = nonEmptySections.get(j);
        var commits = sections.get(section);

        sb.append("          \"").append(section).append("\": [\n");
        for (int k = 0; k < commits.size(); k++) {
          var commit = commits.get(k).replace("\"", "\\\"");
          sb.append("            \"").append(commit).append("\"");
          if (k < commits.size() - 1) {
            sb.append(",");
          }
          sb.append("\n");
        }
        sb.append("          ]");
        if (j < nonEmptySections.size() - 1) {
          sb.append(",");
        }
        sb.append("\n");
      }

      sb.append("        }\n");
      sb.append("      }");
      if (i < versions.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }

    sb.append("    ]\n");
    sb.append("  }\n");
    sb.append("}\n");

    return sb.toString();
  }

  protected void writeToFile(String content, String outputFile) throws IOException {
    try (FileWriter writer = new FileWriter(outputFile)) {
      writer.write(content);
    }
  }
}
