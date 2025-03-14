package dev.buildcli.core.utils.formatter;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatterTest {

    @Test
    void testGenerate_withEmptyData() {
        // Test with empty data map
        JsonFormatter formatter = new JsonFormatter();
        Map<String, Map<String, List<String>>> data = new HashMap<>();

        String result = formatter.generate(data);

        // Check if the JSON contains the expected structure
        assertTrue(result.contains("{"));
        assertTrue(result.contains("\"changelog\": {"));
        assertTrue(result.contains("\"versions\": ["));
        assertFalse(result.contains("\"sections\":"));
    }

    @Test
    void testGenerate_withData() {
        // Test with some data
        JsonFormatter formatter = new JsonFormatter();

        // Sample data
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("Added", Arrays.asList("New feature added"));
        versionData.put("Fixed", Arrays.asList("Bug fixed"));
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check if the basic JSON structure is present
        assertTrue(result.contains("{"));
        assertTrue(result.contains("\"changelog\": {"));
        assertTrue(result.contains("\"versions\": ["));
        assertTrue(result.contains("\"version\": \"1.0.0\""));
        assertTrue(result.contains("\"date\": \"" + LocalDate.now() + "\""));
        assertTrue(result.contains("\"Added\""));
        assertTrue(result.contains("\"New feature added\""));
        assertTrue(result.contains("\"Fixed\""));
        assertTrue(result.contains("\"Bug fixed\""));
    }

    @Test
    void testGenerate_withNoCommitsForSection() {
        // Test with data but no commits for a particular section
        JsonFormatter formatter = new JsonFormatter();

        // Sample data where "Fixed" section has no commits
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("Added", Arrays.asList("New feature added"));
        versionData.put("Fixed", Collections.emptyList());  // No commits for "Fixed"
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check that the basic structure is present
        assertTrue(result.contains("{"));
        assertTrue(result.contains("\"changelog\": {"));
        assertTrue(result.contains("\"versions\": ["));
        assertTrue(result.contains("\"version\": \"1.0.0\""));
        assertTrue(result.contains("\"Added\""));
        assertTrue(result.contains("\"New feature added\""));

        // Ensure that the "Fixed" section is not included in the JSON
        assertFalse(result.contains("\"Fixed\":"));
    }

    @Test
    void testGenerate_withMultipleVersions() {
        // Test with multiple versions and different commit messages
        JsonFormatter formatter = new JsonFormatter();

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

        // Check for the presence of both versions in the JSON
        assertTrue(result.contains("\"version\": \"1.0.0\""));
        assertTrue(result.contains("\"Added\""));
        assertTrue(result.contains("\"New feature added\""));
        assertTrue(result.contains("\"Fixed\""));
        assertTrue(result.contains("\"Bug fixed\""));
        assertTrue(result.contains("\"version\": \"1.1.0\""));
        assertTrue(result.contains("\"Added\""));
        assertTrue(result.contains("\"New endpoint added\""));
    }

    @Test
    void testGenerate_withInvalidSection() {
        // Test with invalid section in the data
        JsonFormatter formatter = new JsonFormatter();

        // Sample data with invalid section
        Map<String, Map<String, List<String>>> data = new HashMap<>();
        Map<String, List<String>> versionData = new HashMap<>();
        versionData.put("InvalidSection", Arrays.asList("Invalid commit"));
        data.put("1.0.0", versionData);

        String result = formatter.generate(data);

        // Check if the basic structure is present
        assertTrue(result.contains("{"));
        assertTrue(result.contains("\"changelog\": {"));
        assertTrue(result.contains("\"versions\": ["));
        assertTrue(result.contains("\"version\": \"1.0.0\""));

        // Check that the invalid section is not included in the JSON
        assertFalse(result.contains("\"InvalidSection\":"));
    }
}
