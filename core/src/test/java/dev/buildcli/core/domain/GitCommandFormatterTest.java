package dev.buildcli.core.domain;

import dev.buildcli.core.domain.git.GitCommandFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GitCommandFormatterTestHelper extends GitCommandFormatter {
    static String callGetGitDir(String path) {
        return getGitDir(path);
    }
    static String callGetWorkTree(String path) {
        return getWorkTree(path);
    }
    static String callReleaseVersion(String version) {
        return releaseVersion(version);
    }
    static String callDistinctContributors(Iterable<RevCommit> commits) {
        return distinctContributors(commits);
    }
    static Long callCountLogs(Iterable<RevCommit> logs) {
        return countLogs(logs);
    }
}

public class GitCommandFormatterTest {

    @Test
    void testGetGitDir() {
        String path = "/home/user/repo";
        String expected = "--git-dir=/home/user/repo/.git";
        assertEquals(expected, GitCommandFormatterTestHelper.callGetGitDir(path));
    }

    @Test
    void testGetWorkTree() {
        String path = "/home/user/repo";
        String expected = "--work-tree=/home/user/repo";
        assertEquals(expected, GitCommandFormatterTestHelper.callGetWorkTree(path));
    }

    @Test
    void testReleaseVersion() {
        assertEquals("release/v1.2.3", GitCommandFormatterTestHelper.callReleaseVersion("1.2.3"));
    }

    @Test
    void testDistinctContributors_excludesDependabot() {
        RevCommit commit1 = mock(RevCommit.class);
        RevCommit commit2 = mock(RevCommit.class);
        RevCommit commit3 = mock(RevCommit.class);

        PersonIdent author1 = mock(PersonIdent.class);
        PersonIdent author2 = mock(PersonIdent.class);
        PersonIdent author3 = mock(PersonIdent.class);

        when(commit1.getAuthorIdent()).thenReturn(author1);
        when(commit2.getAuthorIdent()).thenReturn(author2);
        when(commit3.getAuthorIdent()).thenReturn(author3);

        when(author1.getName()).thenReturn("Alice");
        when(author2.getName()).thenReturn("Bob");
        when(author3.getName()).thenReturn("dependabot[bot]");

        List<RevCommit> commits = List.of(commit1, commit2, commit3, commit2); // Bob twice

        String result = GitCommandFormatterTestHelper.callDistinctContributors(commits);
        assertEquals("Alice, Bob", result);
    }

    @Test
    void testCountLogs() {
        RevCommit commit1 = mock(RevCommit.class);
        RevCommit commit2 = mock(RevCommit.class);
        RevCommit commit3 = mock(RevCommit.class);

        List<RevCommit> commits = List.of(commit1, commit2, commit3);

        assertEquals(3L, GitCommandFormatterTestHelper.callCountLogs(commits));
    }
}
