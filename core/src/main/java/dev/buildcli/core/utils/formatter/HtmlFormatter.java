package dev.buildcli.core.utils.formatter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static dev.buildcli.core.constants.ChangelogConstants.ORDERED_SECTIONS;

public class HtmlFormatter implements Formatter {

    @Override
    public String generate(Map<String, Map<String, List<String>>> data) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Changelog</title>
                    </head>
                    <body>
                        <h1>Changelog</h1>
                        <p>All notable changes to this project will be documented in this file.<br>
                        The format is based on <a href="https://keepachangelog.com/en/1.0.0/">Keep a Changelog</a>,<br>
                        and this project adheres to <a href="https://semver.org/spec/v2.0.0.html">Semantic Versioning</a>.</p>
                    """);

        data.forEach((version, sectionMap) -> {
            sb.append("<h2>[%s] - %s</h2>\n".formatted(version, LocalDate.now()));

            ORDERED_SECTIONS.forEach(section -> {
                var commits = sectionMap.get(section);
                if (commits != null && !commits.isEmpty()) {
                    sb.append("<h3>%s</h3>\n<ul>\n".formatted(section));
                    commits.forEach(commitMsg -> sb.append("  <li>%s</li>\n".formatted(commitMsg)));
                    sb.append("</ul>\n");
                }
            });
        });

        sb.append("""
            </body>
            </html>
            """);

        return sb.toString();
    }
}
