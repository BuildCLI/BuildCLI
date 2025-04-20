package dev.buildcli.cli.utilsfortest;

import java.util.List;

public class ConfigKeys {

    public static final String LOGGING_BANNER_ENABLE = "buildcli.logging.banner.enabled";
    public static final String LOGGING_BANNER_PATH   = "buildcli.logging.banner.path";
    public static final String AI_VENDOR             = "buildcli.ai.vendor";
    public static final String AI_MODEL              = "buildcli.ai.model";
    public static final String AI_URL                = "buildcli.ai.url";
    public static final String AI_TOKEN              = "buildcli.ai.token";
    public static final String PLUGIN_PATHS          = "buildcli.plugin.paths";

    public static final List<String> ALL_KEYS = List.of(
            LOGGING_BANNER_ENABLE,
            LOGGING_BANNER_PATH,
            AI_VENDOR,
            AI_MODEL,
            AI_URL,
            AI_TOKEN,
            PLUGIN_PATHS
    );

    private ConfigKeys() {}
}