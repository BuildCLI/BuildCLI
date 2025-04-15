package dev.buildcli.core.utils.formatter;

import java.util.List;
import java.util.Map;

public interface Formatter {
    String generate(Map<String, Map<String, List<String>>> data);
}
