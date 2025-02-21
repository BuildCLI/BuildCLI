package dev.buildcli.core.domain.git;

import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.tools.CLIInteractions;
import org.eclipse.jgit.revwalk.RevCommit;


import java.util.logging.Logger;

import static dev.buildcli.core.domain.git.GitCommandFormatter.countLogs;
import static dev.buildcli.core.domain.git.GitCommandFormatter.distinctContributors;

class GitCommandUtils extends GitOperations {
    protected static final Logger logger = Logger.getLogger(GitCommandUtils.class.getName());



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

        SystemOutLogger.log("Contributors: "+ distinctContributors(contributors));

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