package dev.buildcli.core.actions.ai.factories;

import dev.buildcli.core.actions.ai.AIService;
import dev.buildcli.core.actions.ai.AIServiceParams;

public interface AIServiceFactory<S extends AIService, T extends AIServiceParams> {
  S create(T params);
}
