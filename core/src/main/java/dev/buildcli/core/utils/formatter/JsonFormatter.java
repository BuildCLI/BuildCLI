package dev.buildcli.core.utils.formatter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static dev.buildcli.core.constants.ChangelogConstants.ORDERED_SECTIONS;

public class JsonFormatter implements Formatter {

    @Override
    public String generate(Map<String, Map<String, List<String>>> data) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"changelog\": {\n");
        sb.append("    \"description\": \"All notable changes to this project will be documented in this file." +
                " The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.\",\n");
        sb.append("    \"versions\": [\n");

        var versions = data.entrySet().stream().toList();
        for (int i = 0; i < versions.size(); i++) {
            var entry = versions.get(i);
            var version = entry.getKey();
            var sections = entry.getValue();

            sb.append("      {\n");
            sb.append("        \"version\": \"").append(version).append("\",\n");
            sb.append("        \"date\": \"").append(LocalDate.now()).append("\",\n");
            sb.append("        \"sections\": {\n");

            var nonEmptySections = ORDERED_SECTIONS.stream()
                    .filter(section -> sections.containsKey(section) && !sections.get(section).isEmpty())
                    .toList();

            for (int j = 0; j < nonEmptySections.size(); j++) {
                var section = nonEmptySections.get(j);
                var commits = sections.get(section);

                sb.append("          \"").append(section).append("\": [\n");
                for (int k = 0; k < commits.size(); k++) {
                    var commit = commits.get(k).replace("\"", "\\\"");
                    sb.append("            \"").append(commit).append("\"");
                    if (k < commits.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append("          ]");
                if (j < nonEmptySections.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }

            sb.append("        }\n");
            sb.append("      }");
            if (i < versions.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("    ]\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }
}
