package dev.buildcli.core.utils.formatter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static dev.buildcli.core.constants.ChangelogConstants.ORDERED_SECTIONS;

public class MarkdownFormatter implements Formatter {

    @Override
    public String generate(Map<String, Map<String, List<String>>> data) {
        var sb = new StringBuilder();
        sb.append("""
                    # Changelog
        
                    All notable changes to this project will be documented in this file.
        
                    The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
                    and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
        
                    """);

        data.forEach((version, sections) -> {
            sb.append("## [%s] - %s%n%n" .formatted(version, LocalDate.now()));

            ORDERED_SECTIONS.forEach(section -> {
                var commits = sections.get(section);
                if (commits != null && !commits.isEmpty()){
                    sb.append("### %s%n" .formatted(section));
                    commits.forEach(commitsMsg -> sb.append("- %s%n" .formatted(commitsMsg)));
                    sb.append("\n");
                }
            });
        });

        return sb.toString();
    }
}
