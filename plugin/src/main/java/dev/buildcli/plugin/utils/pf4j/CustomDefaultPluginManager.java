package dev.buildcli.plugin.utils.pf4j;

import dev.buildcli.core.domain.jar.Jar;
import org.pf4j.DefaultPluginManager;

import java.util.List;

public class CustomDefaultPluginManager extends DefaultPluginManager {
  public CustomDefaultPluginManager(List<Jar> jars) {
    super();
    for (var jar : jars) {
      loadPlugin(jar.getFile().toPath());
    }
  }
}
