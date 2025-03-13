package dev.buildcli.core.domain.git;

import dev.buildcli.core.utils.tools.CLIInteractions;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static dev.buildcli.core.domain.git.GitCommandFormatter.countLogs;
import static dev.buildcli.core.domain.git.GitCommandFormatter.distinctContributors;

class GitCommandUtils extends GitOperations {
    private static final Logger logger = LoggerFactory.getLogger(GitCommandUtils.class);


    protected void updateLocalRepositoryFromUpstreamWithStash(String path, String url){
        startGitRepository(path);

        if(!isRemoteDefined("upstream")){
            setUpstreamUrl(url);
        }
        
        if (thereIsLocalChanges()){
            boolean eraserLocalChanges = !CLIInteractions.getConfirmation("eraser local changes");
            if( !eraserLocalChanges){
                stashChanges();
                pullUpstream();
                popStash();
            }
        }else{
            pullUpstream();
        }
        
        closeGitRepository();
    }

    protected void getContributors(String gitPath, String url) {
        startGitRepository(gitPath);

        setRemoteUrl(url);

        gitFetch();

        getCommit(checkLocalHeadCommits());
        getCommit(checkRemoteHeadCommits());

        Iterable<RevCommit> contributors = gitLog();

        logger.info("Contributors: {}", distinctContributors(contributors));

        closeGitRepository();
    }

    protected boolean isRepositoryUpdatedUpstream(String gitPath, String url){
        startGitRepository(gitPath);

        setRemoteUrl(url);
        gitFetch();

        RevCommit local = getCommit(checkLocalHeadCommits());
        RevCommit remote =getCommit(checkRemoteHeadCommits());

        int count = Math.toIntExact(countLogs(
                gitLogOnlyCommitsNotInLocal(local
                        ,remote)
        ));

        return count == 0;
    }
}