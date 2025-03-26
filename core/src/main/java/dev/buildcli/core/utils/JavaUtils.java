package dev.buildcli.core.utils;

public abstract class JavaUtils {

    private JavaUtils() {}

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

}
