package dev.buildcli.core.utils;

import java.util.HashMap;
import java.util.Map;

public class LanguageDetector {
  private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();

  static {
    LANGUAGE_MAP.put(".java", "java");
    LANGUAGE_MAP.put(".py", "python");
    LANGUAGE_MAP.put(".js", "javascript");
    LANGUAGE_MAP.put(".ts", "typescript");
    LANGUAGE_MAP.put(".cpp", "c++");
    LANGUAGE_MAP.put(".c", "c");
    LANGUAGE_MAP.put(".cs", "c#");
    LANGUAGE_MAP.put(".rb", "ruby");
    LANGUAGE_MAP.put(".php", "php");
    LANGUAGE_MAP.put(".swift", "swift");
    LANGUAGE_MAP.put(".go", "go");
    LANGUAGE_MAP.put(".rs", "rust");
    LANGUAGE_MAP.put(".kt", "kotlin");
    LANGUAGE_MAP.put(".dart", "dart");
    LANGUAGE_MAP.put(".r", "r");
    LANGUAGE_MAP.put(".sh", "shell script");
  }

  public static String detectLanguage(String fileName) {
    for (Map.Entry<String, String> entry : LANGUAGE_MAP.entrySet()) {
      if (fileName.endsWith(entry.getKey())) {
        return entry.getValue();
      }
    }
    return "Unknown";
  }
}
