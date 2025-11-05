package dev.buildcli.core.constants;

import dev.buildcli.core.utils.OS;

public class MavenConstants {
  public static final String MAVEN_CMD_WINDOWS = "mvn.cmd";
  public static final String MAVEN_CMD_UNIX = "mvn";
  public static final String MAVEN_CMD = OS.isWindows() ? MAVEN_CMD_WINDOWS : MAVEN_CMD_UNIX;
  public static final String FILE = "pom.xml";
  public static final String DEPENDENCIES_PATTERN = "##dependencies##";
  public static final String TARGET = "target";
  public static final String ADDED_COMMENT = "Added by BuildCLI";
}
