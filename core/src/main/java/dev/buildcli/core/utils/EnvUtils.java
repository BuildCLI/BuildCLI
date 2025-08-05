package dev.buildcli.core.utils;

public class EnvUtils {
  private EnvUtils() {}
  public static String getEnv(String env){
    return System.getenv(env);
  }
}
