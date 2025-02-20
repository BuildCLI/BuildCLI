package org.buildcli.actions.tools;

import java.util.LinkedList;
import java.util.List;

public class ToolCheckers {
  private static final List<ToolChecker> TOOL_CHECKERS = new LinkedList<>();

  static {
    TOOL_CHECKERS.add(new JDKChecker());
    TOOL_CHECKERS.add(new MavenChecker());
    TOOL_CHECKERS.add(new GradleChecker());
    TOOL_CHECKERS.add(new DockerChecker());
  }

  public static List<ToolChecker> all() {
    return TOOL_CHECKERS;
  }
}
