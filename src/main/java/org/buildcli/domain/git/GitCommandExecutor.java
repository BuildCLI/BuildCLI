package org.buildcli.domain.git;

import org.buildcli.log.SystemOutLogger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import static org.buildcli.domain.git.GitCommands.*;
import static org.buildcli.domain.git.GitCommandFormatter.*;

public class GitCommandExecutor {

    static {
        // Set Apache MINA SSHD as the SSH session factory
        SshdSessionFactory factory = new SshdSessionFactory();
        SshdSessionFactory.setInstance(factory);
    }
    private static final Logger logger = Logger.getLogger(GitCommandExecutor.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GitCommandExecutor.class);

    private Git git;
    private Repository repository;

    private Git openGitRepository(String path) {
        try {
            File repoDir = new File(path);
            git = Git.open(repoDir);
            return git;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error opening Git repository", e);
            throw new RuntimeException(e);
        }
    }

    private void setRemoteUrl(String url){
        try {
            git.remoteSetUrl().setRemoteUri(new URIish(url));
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Error setting Git remote url", e);
            throw new RuntimeException(e);
        }
    }

    private void gitFetch(){
        try {
            git.fetch().call();
        } catch (GitAPIException e) {
            logger.log(Level.SEVERE, "Error executing git fetch command", e);
            throw new RuntimeException(e);
        }
    }

    private Repository getRepository(){
        return git.getRepository();
    }

    private ObjectId checkRemoteHeadCommits(){
        try {
            ObjectId remoteHead = repository.resolve("origin/main^{tree}");
            if (remoteHead != null){
                return remoteHead;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error resolving remote Head commits");
        }
        return null;
    }

    private ObjectId checkLocalHeadCommits(){
        try {
            ObjectId localHead = repository.resolve("HEAD^{tree}");
            if (localHead != null){
                return localHead;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error resolving local Head commits");
        }
        return null;
    }

    private void closeGitRepository(Git git){
        git.close();
    }

    protected int runGitCommand(String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, String.format("Git command '%s' failed.", String.join(" ", command)), e);
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    protected void runGitCommandWithException(String... command) throws IOException {
        int exitCode = runGitCommand(command);
        if (exitCode != 0) {
            throw new IOException(String.format("Git command '%s' failed. Ensure you are in a Git repository and the command is valid.", String.join(" ", command)));
        }
    }

    protected String runGitCommandAndShowOutput(String... command) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return output.toString();
    }

    private Iterable<RevCommit> gitLog(){
        try {
            return git.log().addPath("src/main/java").call();
        } catch (GitAPIException e) {
            logger.log(Level.SEVERE, "Error executing git log command", e);
            throw new RuntimeException(e);
        }
    }

    private Iterable<RevCommit> gitgitLogOnlyCommitsNotInLocal(ObjectId localHead, ObjectId remoteHead){
        try {
            return git.log()
                    .not(localHead)
                    .add(remoteHead)
                    .call();
        } catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException e) {
            logger.log(Level.SEVERE, "Error executing git log command", e);
            throw new RuntimeException(e);
        }
    }



    public void showContributors(String gitPath) {
        git = openGitRepository(gitPath);
        setRemoteUrl("https://github.com/wheslleyrimar/BuildCLI.git");

        gitFetch();

        repository = getRepository();

        checkLocalHeadCommits();
        checkRemoteHeadCommits();

        Iterable<RevCommit> contributors = gitLog();

        SystemOutLogger.log("Contributors: "+ distinctContributors(contributors));

        closeGitRepository(git);
    }

    public String getDirectory() {
        return System.getProperty("user.dir");
    }

    public String findGitRepository(String path) {
        try {
            File dir = new File(path);
            while (dir != null && dir.exists()) {
                if (new File(dir, ".git").exists()) {
                    return dir.getCanonicalPath();
                }
                dir = dir.getParentFile();
            }
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Git repository not found!");
            throw new RuntimeException(e);
        }
    }


    public boolean checkIfLocalRepositoryIsUpdated(String gitPath){
        String gitDir = getGitDir(gitPath), workTree = getWorkTree(gitPath);

        runGitCommand(GIT, gitDir, workTree, FETCH, ORIGIN_MAIN);
        String gitRevListCountResult = runGitCommandAndShowOutput(GIT, gitDir, workTree,REV_LIST,COUNT,HEAD+RANGE+ORIGIN_MAIN);
        return Integer.parseInt(gitRevListCountResult.trim()) == 0;
    }

    public void updateLocalRepository(){
        String s = runGitCommandAndShowOutput(GIT, PULL, ORIGIN_MAIN);
        SystemOutLogger.log(s);
    }

    public void updateLocalRepository(String localRepository){
        String gitDir = getGitDir(localRepository), workTree = getWorkTree(localRepository);

        String s = runGitCommandAndShowOutput(GIT, gitDir, workTree, PULL, ORIGIN_MAIN);
        SystemOutLogger.log(s);
    }

    public void createReleaseBranch(String version) throws IOException, InterruptedException {
        String branchName = releaseVersion(version);
        runGitCommandWithException(GIT, CHECKOUT_B, branchName);
        logger.info(String.format("Created and switched to branch %s", branchName));
    }

    public void createGitTag(String version) throws IOException, InterruptedException {
        runGitCommandWithException(GIT, TAG, "-a", "v" + version, "-m", "Release " + version);
        logger.info(String.format("Created tag v%s", version));
    }

    public void pushReleaseBranch(String version) throws IOException, InterruptedException {
        String branchName = releaseVersion(version);
        runGitCommandWithException(GIT, PUSH_U, ORIGIN, branchName);
        runGitCommandWithException(GIT, PUSH, ORIGIN, "v" + version);
        logger.info(String.format("Pushed branch %s and tag v%s to remote", branchName, version));
    }

    public boolean isGitRepository() {
        return runGitCommand(GIT, REV_PARSE, "--is-inside-work-tree") == 0;
    }

    public boolean hasCommits() {
        return runGitCommand(GIT, LOG, ONELINE) == 0;
    }

    public boolean tagExists(String version) {
        return runGitCommand(GIT, REV_PARSE, "v" + version) == 0;
    }
}
