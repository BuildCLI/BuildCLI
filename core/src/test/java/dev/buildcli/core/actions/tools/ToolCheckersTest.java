package dev.buildcli.core.actions.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ToolCheckersTest {

  @Test
  void testAll_ReturnsAllToolCheckers() {
    List<ToolChecker> toolCheckerList = ToolCheckers.all();

    assertEquals(4, toolCheckerList.size());
    assertEquals(JDKChecker.class, toolCheckerList.get(0).getClass());
    assertEquals(MavenChecker.class, toolCheckerList.get(1).getClass());
    assertEquals(GradleChecker.class, toolCheckerList.get(2).getClass());
    assertEquals(DockerChecker.class, toolCheckerList.get(3).getClass());
  }
}
