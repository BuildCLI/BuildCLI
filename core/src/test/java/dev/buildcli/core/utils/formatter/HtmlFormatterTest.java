package dev.buildcli.core.utils.formatter;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HtmlFormatterTest {

    @Test
    void testGenerate_withEmptyData() {
        // Test with empty data map
        HtmlFormatter formatter = new HtmlFormatter();
        Map<String, Map<String, List<String>>> data = new HashMap<>();

        String result = formatter.generate(data);

        // Check that the generated HTML contains the basic structure
        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<h1>Changelog</h1>"));
        assertTrue(result.contains("<p>All notable changes to this project will be documented in this file."));
        // Ensure that no changelog sections are added
        assertFalse(result.contains("<h2>"));
    }

    @Test
    void testGenerate_withData() {
        // Test with some data
        HtmlFormatter formatter = new HtmlFormatter();

        // Sample data
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("Added", Arrays.asList("New feature added"));
        versionData.put("Fixed", Arrays.asList("Bug fixed"));
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check that the basic structure is present
        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<h1>Changelog</h1>"));
        assertTrue(result.contains("<p>All notable changes to this project will be documented in this file."));

        // Check that version and commit messages appear in the generated HTML
        assertTrue(result.contains("<h2>[1.0.0] - " + LocalDate.now()));
        assertTrue(result.contains("<h3>Added</h3>"));
        assertTrue(result.contains("<li>New feature added</li>"));
        assertTrue(result.contains("<h3>Fixed</h3>"));
        assertTrue(result.contains("<li>Bug fixed</li>"));
    }

    @Test
    void testGenerate_withNoCommitsForSection() {
        // Test with data but no commits for a particular section
        HtmlFormatter formatter = new HtmlFormatter();

        // Sample data where "Fixed" section has no commits
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("Added", Arrays.asList("New feature added"));
        versionData.put("Fixed", Collections.emptyList());  // No commits for "Fixed"
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check that the basic structure is present
        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<h1>Changelog</h1>"));
        assertTrue(result.contains("<p>All notable changes to this project will be documented in this file."));

        // Ensure that the "Fixed" section is not included
        assertTrue(result.contains("<h2>[1.0.0] - " + LocalDate.now()));
        assertTrue(result.contains("<h3>Added</h3>"));
        assertTrue(result.contains("<li>New feature added</li>"));
        assertFalse(result.contains("<h3>Fixed</h3>"));
    }

    @Test
    void testGenerate_withMultipleVersions() {
        // Test with multiple versions and different commit messages
        HtmlFormatter formatter = new HtmlFormatter();

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

        // Check for the presence of both versions in the HTML
        assertTrue(result.contains("<h2>[1.0.0] - " + LocalDate.now()));
        assertTrue(result.contains("<li>New feature added</li>"));
        assertTrue(result.contains("<h2>[1.1.0] - " + LocalDate.now()));
        assertTrue(result.contains("<li>New endpoint added</li>"));
    }

    @Test
    void testGenerate_withInvalidSection() {
        // Test with invalid section in the data
        HtmlFormatter formatter = new HtmlFormatter();

        // Sample data with invalid section
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("InvalidSection", Arrays.asList("Invalid commit"));
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check that the basic structure is present
        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<h1>Changelog</h1>"));
        assertTrue(result.contains("<p>All notable changes to this project will be documented in this file."));

        // Check that the invalid section is not included in the HTML
        assertTrue(result.contains("<h2>[1.0.0] - " + LocalDate.now()));
        assertFalse(result.contains("<h3>InvalidSection</h3>"));
    }
}

