package org.buildcli.domain.git;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.stream.StreamSupport;

public class GitCommandFormatter {

    protected static String getGitDir(String localRepository){
        return "--git-dir=" + localRepository +"/.git";
    }

    protected static String getWorkTree(String localRepository){
        return  "--work-tree=" +localRepository;
    }

    protected static String releaseVersion(String version){
        return "release/v" + version;
    }

    protected static String distinctContributors(Iterable<RevCommit> contributors){
        return StreamSupport.stream(contributors.spliterator(), false)
                .map(revCommit -> revCommit.getAuthorIdent().getName())
                .distinct()
                .filter(name -> !name.contains("dependabot"))
                .toList().toString().replaceAll("[\\[\\]]", "");
    }

    protected static Long countLogs(Iterable<RevCommit> logs){
        return StreamSupport.stream(logs.spliterator(), false)
                .count();
    }
}
