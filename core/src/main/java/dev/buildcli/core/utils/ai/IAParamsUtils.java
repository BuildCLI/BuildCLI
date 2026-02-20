package dev.buildcli.core.utils.ai;

import dev.buildcli.core.actions.ai.AIServiceParams;
import dev.buildcli.core.actions.ai.params.GeminiAIServiceParams;
import dev.buildcli.core.actions.ai.params.JlamaAIServiceParams;
import dev.buildcli.core.actions.ai.params.OllamaAIServiceParams;
import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.utils.config.ConfigContextLoader;

public final class IAParamsUtils {
  private static final BuildCLIConfig allConfigs = ConfigContextLoader.getAllConfigs();

  private IAParamsUtils() {
  }

  public static AIServiceParams createAIParams() {
    var aiVendor = allConfigs.getProperty(ConfigDefaultConstants.AI_VENDOR).orElse("jlama");
    var aiModel = allConfigs.getProperty(ConfigDefaultConstants.AI_MODEL).orElse(null);
    var aiToken = allConfigs.getProperty(ConfigDefaultConstants.AI_TOKEN).orElse(null);
    var aiUrl = allConfigs.getProperty(ConfigDefaultConstants.AI_URL).orElse(null);

    return switch (aiVendor.toLowerCase()) {
      case "ollama" -> new OllamaAIServiceParams(aiUrl, aiModel);
      case "jlama" -> new JlamaAIServiceParams(aiModel);
      case "gemini" -> new GeminiAIServiceParams(aiModel, aiToken);
      default -> throw new IllegalStateException("Unexpected AI Vendor: " + aiVendor);
    };
  }
}
