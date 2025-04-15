package dev.buildcli.core.domain.git;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.buildcli.core.domain.git.GitCommands.*;
import static dev.buildcli.core.domain.git.GitCommandFormatter.*;

public class GitCommandExecutor extends GitCommandUtils {

    private static final Logger logger = Logger.getLogger(GitCommandExecutor.class.getName());

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

    public void updateLocalRepositoryFromUpstream(String gitPath, String url) {
        updateLocalRepositoryFromUpstreamWithStash(gitPath, url);
    }

    public String showContributors() {
        return getContributorsFromAPI();
    }

    // TODO: improve the hotfix below

    private static String getContributorsFromAPI() {
        String url = "https://api.github.com/repos/BuildCLI/BuildCLI/contributors";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = null;
        try {
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            client.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        JsonArray json = new Gson().fromJson(response.body(), JsonArray.class);

        return json.asList().stream()
                .map(JsonObject.class::cast)
                .map(jsonObject -> jsonObject.get("login").getAsString())
                .filter(login -> !login.equals("wrimar") && !login.equals("dependabot[bot]"))
                .reduce((s1, s2) -> s1 + ", " + s2)
                .orElse("");
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

    public boolean checkIfLocalRepositoryIsUpdated(String gitPath, String url){
        return isRepositoryUpdatedUpstream(gitPath, url);
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
