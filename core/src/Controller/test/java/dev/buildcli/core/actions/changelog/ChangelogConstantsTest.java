package dev.buildcli.core.actions.changelog;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ChangelogConstantsTest {

    @Test
    void testCommitPatternSuccess_1() {
        Pattern pattern = ChangelogConstants.COMMIT_PATTERN;

        Matcher matcher = pattern.matcher("test(test): test");
        assertTrue(matcher.matches());
    }

    @Test
    void testCommitPatternSuccess_2() {
        Pattern pattern = ChangelogConstants.COMMIT_PATTERN;

        Matcher matcher = pattern.matcher("test: test");
        assertTrue(matcher.matches());
    }

    @Test
    void testCommitPatternFail_1() {
        Pattern pattern = ChangelogConstants.COMMIT_PATTERN;

        Matcher matcher = pattern.matcher("test");
        assertFalse(matcher.matches());
    }

    @Test
    void typeToKeepSectionMappingSuccess() {
        assertEquals("Added", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("feat"));
        assertEquals("Added", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("docs"));
        assertEquals("Fixed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("fix"));
        assertEquals("Changed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("refactor"));
        assertEquals("Changed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("perf"));
        assertEquals("Changed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("chore"));
        assertEquals("Changed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("style"));
        assertEquals("Changed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("ci"));
        assertEquals("Changed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("build"));
        assertEquals("Changed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("test"));
        assertEquals("Deprecated", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("deprecated"));
        assertEquals("Removed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("remove"));
        assertEquals("Removed", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("revert"));
        assertEquals("Security", ChangelogConstants.TYPE_TO_KEEP_SECTION.get("security"));
        assertNull(ChangelogConstants.TYPE_TO_KEEP_SECTION.get("unknown"));
    }
}
