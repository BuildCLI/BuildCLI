package dev.buildcli.core.domain.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jgit.util.StringUtils;


import java.net.URISyntaxException;
import java.io.File;
import java.io.IOException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;


public class GitOperations {
    private static final Logger logger = LoggerFactory.getLogger(GitOperations.class);

    public Git git;
    public Repository repository;

    static {
        // Set Apache MINA SSHD as the SSH session factory
        SshdSessionFactory factory = new SshdSessionFactory();
        SshdSessionFactory.setInstance(factory);
    }

    public Git openGitRepository(String path) {
        try {
            git = Git.open(new File(path));
            repository = git.getRepository();
            return git;
        } catch (IOException e) {
            handleException("Error opening Git repository", e);
            return null;
        }
    }

    public void closeGitRepository() {
        git.close();
    }

    protected Repository getRepository(){
        return git.getRepository();
    }

    public void startGitRepository(String path){
        git = openGitRepository(path);
        repository = getRepository();
    }

    public void setRemoteUrl(String url){
        try {
            git.remoteSetUrl().setRemoteUri(new URIish(url)).call();
        } catch (URISyntaxException | GitAPIException e) {
            handleException("Error setting Git remote url", e);
        }
    }

    protected void stashChanges(){
        try {
            git.stashCreate().call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean thereIsLocalChanges(){
        try {
            return !git.status().call().isClean();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    protected void popStash(){
        try {
            git.stashApply().call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUpstreamUrl(String url){
        try {
            git.remoteAdd().setName("upstream").setUri(new URIish(url)).call();
        } catch (URISyntaxException | GitAPIException e) {
            handleException("Error setting Git remote upstream url", e);
        }
    }

    public void gitFetch() {
        try {
            git.fetch().call();
        } catch (GitAPIException e) {
            handleException("Error executing git fetch command", e);
        }
    }

    public ObjectId checkRemoteHeadCommits() {
        return checkHeadCommits("origin/main");
    }

    public ObjectId checkLocalHeadCommits() {
        return checkHeadCommits("HEAD");
    }

    private ObjectId checkHeadCommits(String branch) {
        try {
            return repository.resolve(branch);
        } catch (IOException e) {
            handleException("Error resolving Head commits", e);
            return null;
        }
    }

    public RevCommit getCommit(ObjectId objectId){
        try (RevWalk walk = new RevWalk(repository)) {
            return walk.parseCommit(objectId);
        } catch (IOException e) {
            handleException("Error parsing commit", e);
            return null;
        }
    }

    public Iterable<RevCommit> gitLog(String path) {
        try {
            if (!StringUtils.isEmptyOrNull(path)) {
                return git.log().addPath(path).call();
            }

            return git.log().call();
        } catch (GitAPIException e) {
            handleException("Error executing git log command", e);
            return null;
        }
    }

    public List<Ref> getTagList() {
        try {
            return git.tagList().call();
        } catch (GitAPIException e) {
            handleException("Error executing git log command", e);
            return null;
        }
    }

    public Optional<String> getLatestGitTag() throws IOException {

        try {
            List<Ref> taglist = getTagList();
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

    public Iterable<RevCommit> gitLogOnlyCommitsNotInLocal(RevCommit localHead, RevCommit remoteHead){
        try {
            return git.log().not(localHead).add(remoteHead).call();
        } catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException e) {
            handleException("Error executing git log command", e);
            return null;
        }
    }

    public boolean isRemoteDefined(String remoteName) {
        return repository.getConfig().getSubsections("remote").contains(remoteName);
    }

    public void pullUpstream() {
        try {
            git.pull().setRemote("upstream").setRemoteBranchName("main").call();
        } catch (GitAPIException e) {
            handleException("Error executing git pull upstream main command", e);
        }
    }

    private void handleException(String message, Exception e) {
        logger.error(message, e);
        throw new RuntimeException(e);
    }
}
