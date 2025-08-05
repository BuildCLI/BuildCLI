package dev.buildcli.core.utils.formatter;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownFormatterTest {

    @Test
    void testGenerate_withEmptyData() {
        // Test with empty data map
        MarkdownFormatter formatter = new MarkdownFormatter();
        Map<String, Map<String, List<String>>> data = new HashMap<>();

        String result = formatter.generate(data);

        // Check if the markdown structure is present
        assertTrue(result.contains("# Changelog"));
        assertTrue(result.contains("All notable changes to this project will be documented in this file."));
        assertFalse(result.contains("###"));
    }

    @Test
    void testGenerate_withData() {
        // Test with some data
        MarkdownFormatter formatter = new MarkdownFormatter();

        // Sample data
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("Added", Arrays.asList("New feature added"));
        versionData.put("Fixed", Arrays.asList("Bug fixed"));
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check if the markdown structure is present
        assertTrue(result.contains("# Changelog"));
        assertTrue(result.contains("All notable changes to this project will be documented in this file."));
        assertTrue(result.contains("## [1.0.0] - " + LocalDate.now()));
        assertTrue(result.contains("### Added"));
        assertTrue(result.contains("- New feature added"));
        assertTrue(result.contains("### Fixed"));
        assertTrue(result.contains("- Bug fixed"));
    }

    @Test
    void testGenerate_withNoCommitsForSection() {
        // Test with data but no commits for a particular section
        MarkdownFormatter formatter = new MarkdownFormatter();

        // Sample data where "Fixed" section has no commits
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("Added", Arrays.asList("New feature added"));
        versionData.put("Fixed", Collections.emptyList());  // No commits for "Fixed"
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check that the basic markdown structure is present
        assertTrue(result.contains("# Changelog"));
        assertTrue(result.contains("All notable changes to this project will be documented in this file."));
        assertTrue(result.contains("## [1.0.0] - " + LocalDate.now()));

        // Ensure that the "Fixed" section is not included in the markdown
        assertFalse(result.contains("### Fixed"));
    }

    @Test
    void testGenerate_withMultipleVersions() {
        // Test with multiple versions and different commit messages
        MarkdownFormatter formatter = new MarkdownFormatter();

        // Sample data for multiple versions
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> version1Data = new HashMap<>();
        version1Data.put("Added", Arrays.asList("New feature added"));
        version1Data.put("Fixed", Arrays.asList("Bug fixed"));
        data.put("1.0.0", version1Data);

        Map<String, List<String>> version2Data = new HashMap<>();
        version2Data.put("Added", Arrays.asList("New endpoint added"));
        data.put("1.1.0", version2Data);

        String result = formatter.generate(data);

        // Check for the presence of both versions in the markdown
        assertTrue(result.contains("## [1.0.0] - " + LocalDate.now()));
        assertTrue(result.contains("### Added"));
        assertTrue(result.contains("- New feature added"));
        assertTrue(result.contains("### Fixed"));
        assertTrue(result.contains("- Bug fixed"));
        assertTrue(result.contains("## [1.1.0] - " + LocalDate.now()));
        assertTrue(result.contains("### Added"));
        assertTrue(result.contains("- New endpoint added"));
    }

    @Test
    void testGenerate_withInvalidSection() {
        // Test with invalid section in the data
        MarkdownFormatter formatter = new MarkdownFormatter();

        // Sample data with an invalid section
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("InvalidSection", Arrays.asList("Invalid commit"));
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check if the basic markdown structure is present
        assertTrue(result.contains("# Changelog"));
        assertTrue(result.contains("All notable changes to this project will be documented in this file."));
        assertTrue(result.contains("## [1.0.0] - " + LocalDate.now()));

        // Ensure that the invalid section is not included in the markdown
        assertFalse(result.contains("### InvalidSection"));
    }
}
