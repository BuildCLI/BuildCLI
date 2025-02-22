package dev.buildcli.core.utils.config;

import dev.buildcli.core.domain.configs.BuildCLIConfig;

public abstract class ConfigContextLoader {
  private static BuildCLIConfig localConfig = null;
  private static BuildCLIConfig globalConfig = null;
  private static BuildCLIConfig mergedConfig = null;

  public static BuildCLIConfig getLocalConfig() {
    if (localConfig == null) {
      localConfig = ConfigsOperationsUtils.getLocal().orElse(BuildCLIConfig.empty());
    }

    return localConfig;
  }

  public static BuildCLIConfig getGlobalConfig() {
    if (globalConfig == null) {
      globalConfig = ConfigsOperationsUtils.getGlobal().orElse(BuildCLIConfig.empty());
    }

    return globalConfig;
  }

  public static BuildCLIConfig getAllConfigs() {
    if (mergedConfig == null) {
      localConfig = getLocalConfig();
      globalConfig = getGlobalConfig();
      mergedConfig = BuildCLIConfig.empty();

      for (var property : globalConfig.getProperties()) {
        mergedConfig.addOrSetProperty(property.name(), property.value());
      }

      for (var property : localConfig.getProperties()) {
        mergedConfig.addOrSetProperty(property.name(), property.value());
      }
    }

    return mergedConfig;
  }

  public static void saveLocalConfig(BuildCLIConfig localConfig) {
    localConfig.setLocal(true);
    ConfigsOperationsUtils.set(localConfig);
  }

  public static void saveGlobalConfig(BuildCLIConfig globalConfig) {
    globalConfig.setLocal(false);
    ConfigsOperationsUtils.set(globalConfig);
  }
}
