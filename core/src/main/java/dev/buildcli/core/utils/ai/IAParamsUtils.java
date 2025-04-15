package dev.buildcli.core.utils.ai;

import dev.buildcli.core.actions.ai.AIServiceParams;
import dev.buildcli.core.actions.ai.params.JlamaAIServiceParams;
import dev.buildcli.core.actions.ai.params.OllamaAIServiceParams;
import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.utils.config.ConfigContextLoader;

public final class IAParamsUtils {
  private static final BuildCLIConfig allConfigs = ConfigContextLoader.getAllConfigs();

  private IAParamsUtils() {
  }

  public static AIServiceParams createAIParams(String model, String vendor) {
    var aiVendor = vendor == null ? allConfigs.getProperty(ConfigDefaultConstants.AI_VENDOR).orElse("jlama") : vendor;
    var aiModel = model;

    return switch (aiVendor.toLowerCase()) {
      case "ollama" -> {
        var url = allConfigs.getProperty(ConfigDefaultConstants.AI_URL).orElse(null);
        aiModel = aiModel == null ? allConfigs.getProperty(ConfigDefaultConstants.AI_MODEL).orElse(null) : aiModel;

        yield new OllamaAIServiceParams(url, aiModel);
      }
      case "jlama" -> {
        aiModel = aiModel == null ? allConfigs.getProperty(ConfigDefaultConstants.AI_MODEL).orElse(null) : aiModel;

        yield new JlamaAIServiceParams(aiModel);
      }
      default -> throw new IllegalStateException("Unexpected AI Vendor: " + aiVendor);
    };
  }

  public static AIServiceParams createAIParams(String model) {
    return createAIParams(model, null);
  }

  public static AIServiceParams createAIParams() {
    return createAIParams(null, null);
  }
}
