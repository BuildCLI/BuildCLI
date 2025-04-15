package dev.buildcli.core.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DependencyTest {

  @Test
  void testEmptyConstructor() {
    Dependency dependency = new Dependency();

    // check default vales
    Assertions.assertNull(dependency.getGroupId());
    Assertions.assertNull(dependency.getArtifactId());
    Assertions.assertNull(dependency.getVersion());
    Assertions.assertNull(dependency.getType());
    Assertions.assertNull(dependency.getScope());
    Assertions.assertNull(dependency.getOptional());

  }

  void testConstructorWith3Args() {
    String groupId = "com.example";
    String artifactId = "my-lib";
    String version = "1.2.3";

    Dependency dependency = new Dependency(groupId, artifactId, version);

    // verify constructor sets fields correctly
    Assertions.assertEquals(groupId, dependency.getGroupId());
    Assertions.assertEquals(artifactId, dependency.getArtifactId());
    Assertions.assertEquals(version, dependency.getVersion());

    // The other fields should be null
    Assertions.assertNull(dependency.getType());
    Assertions.assertNull(dependency.getScope());
    Assertions.assertNull(dependency.getOptional());
  }

  @Test
  void testConstructorWith6Args() {
    String groupId = "com.example";
    String artifactId = "my-lib";
    String version = "1.2.3";
    String type = "jar";
    String scope = "compile";
    String optional = "true";

    Dependency dependency = new Dependency(groupId, artifactId, version, type, scope, optional);

    Assertions.assertEquals(groupId, dependency.getGroupId());
    Assertions.assertEquals(artifactId, dependency.getArtifactId());
    Assertions.assertEquals(version, dependency.getVersion());
    Assertions.assertEquals(type, dependency.getType());
    Assertions.assertEquals(scope, dependency.getScope());
    Assertions.assertEquals(optional, dependency.getOptional());
  }

  @Test
  void testSettersAndGetters() {
    Dependency dependency = new Dependency();

    dependency.setGroupId("org.demo");
    dependency.setArtifactId("demo-lib");
    dependency.setVersion("2.0.1");
    dependency.setType("war");
    dependency.setScope("test");
    dependency.setOptional("false");

    Assertions.assertEquals("org.demo", dependency.getGroupId());
    Assertions.assertEquals("demo-lib", dependency.getArtifactId());
    Assertions.assertEquals("2.0.1", dependency.getVersion());
    Assertions.assertEquals("war", dependency.getType());
    Assertions.assertEquals("test", dependency.getScope());
    Assertions.assertEquals("false", dependency.getOptional());
  }
}