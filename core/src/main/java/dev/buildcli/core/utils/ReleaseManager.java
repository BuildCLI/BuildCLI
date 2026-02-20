package dev.buildcli.core.utils;

import dev.buildcli.core.domain.git.GitCommandExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReleaseManager {

  private static final GitCommandExecutor gitExec = new GitCommandExecutor();

  private static final Logger logger = Logger.getLogger(ReleaseManager.class.getName());
  private static final Path VERSION_FILE = Path.of("VERSION");
  private static final Path CHANGELOG_FILE = Path.of("CHANGELOG.md");

  public void automateRelease() {
    if (!gitExec.isGitRepository()) {
      logger.severe("Not a Git repository. Run 'gitExec init' to initialize and make at least one commit.");
      return;
    }

    try {
      String currentVersion = readVersionFile();

      if (!gitExec.hasCommits()) {
        logger.severe("No commits found in the Git repository. Make at least one commit before releasing.");
        return;
      }

      if (gitExec.tagExists(currentVersion)) {
        logger.severe(String.format("Tag v%s already exists. Delete it or use a different version.", currentVersion));
        return;
      }

      gitExec.createReleaseBranch(currentVersion);
      updateChangelog(currentVersion);
      gitExec.createGitTag(currentVersion);
      gitExec.pushReleaseBranch(currentVersion);

      logger.info(String.format("Release automation completed for version %s", currentVersion));
    } catch (IOException | InterruptedException e) {
      logger.log(Level.SEVERE, "Failed to automate release", e);
    }
  }

  private String readVersionFile() throws IOException {
    if (!Files.exists(VERSION_FILE)) {
      throw new IOException("VERSION file not found. Please create a VERSION file with the current version.");
    }
    return Files.readString(VERSION_FILE).trim();
  }

  private void updateChangelog(String version) throws IOException {
    String changelogEntry = String.format("## Version %s%n- Description of changes%n", version);

    if (!Files.exists(CHANGELOG_FILE)) {
      Files.writeString(CHANGELOG_FILE, "# Changelog\n\n", StandardOpenOption.CREATE);
    }

    Files.writeString(CHANGELOG_FILE, changelogEntry, StandardOpenOption.APPEND);
    logger.info(String.format("Changelog updated for version %s", version));
  }
}
