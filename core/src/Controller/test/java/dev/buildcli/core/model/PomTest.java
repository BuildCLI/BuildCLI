package dev.buildcli.core.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Pom} class.
 *
 * <p>
 * These tests verify adding, removing, and managing dependencies
 * within a Pom instance, as well as related utility methods.
 * </p>
 */
public final class PomTest {

  /**
   * Tests that a new Pom starts with an empty list of dependencies.
   */
  @Test
  void testDefaultConstructor() {
    Pom pom = new Pom();
    Assertions.assertEquals(0, pom.countDependencies(),
        "New Pom should have zero dependencies");
    Assertions.assertTrue(pom.getDependencies().isEmpty(),
        "getDependencies() should return an empty list");
  }

  /**
   * Tests adding a dependency using the 'groupId:artifactId:version' format.
   */
  @Test
  void testAddDependencyStringThreeParts() {
    Pom pom = new Pom();
    pom.addDependency("org.example:my-lib:1.2.3");

    Assertions.assertEquals(1, pom.countDependencies(),
        "Should have exactly 1 dependency after adding");
    Assertions.assertTrue(
        pom.hasDependency("org.example", "my-lib"),
        "HasDependency should return true for org.example:my-lib");
  }

  /**
   * Tests adding a dependency using the 'groupId:artifactId' format,
   * which should default the version to 'LATEST'.
   */
  @Test
  void testAddDependencyStringTwoParts() {
    Pom pom = new Pom();
    pom.addDependency("com.test:demo-lib");

    Assertions.assertEquals(1, pom.countDependencies(),
        "Should have exactly 1 dependency after adding");
    Assertions.assertTrue(
        pom.hasDependency("com.test", "demo-lib"),
        "HasDependency should return true for com.test:demo-lib");

    // Check if version was set to 'LATEST'
    Dependency dep = pom.getDependencies().get(0);
    Assertions.assertEquals("LATEST", dep.getVersion(),
        "Expected 'LATEST' as the default version");
  }

  /**
   * Tests removing a dependency by string format 'groupId:artifactId'.
   */
  @Test
  void testRemoveDependencyString() {
    Pom pom = new Pom();
    pom.addDependency("com.remove:lib:2.0.0");
    Assertions.assertEquals(1, pom.countDependencies(),
        "Should have 1 dependency after adding");

    pom.rmDependency("com.remove:lib");
    Assertions.assertEquals(0, pom.countDependencies(),
        "Should have zero dependencies after removal");
  }

  /**
   * Tests adding a dependency directly by groupId/artifactId/version parameters.
   */
  @Test
  void testAddDependencyParameters() {
    Pom pom = new Pom();
    pom.addDependency("org.test", "artifact-demo", "4.5.6");
    Assertions.assertEquals(1, pom.countDependencies(),
        "Should have 1 dependency");
    Assertions.assertTrue(pom.hasDependency("org.test", "artifact-demo"),
        "Should confirm the newly added dependency exists");
  }

  /**
   * Tests removing a dependency by groupId/artifactId parameters.
   */
  @Test
  void testRemoveDependencyParameters() {
    Pom pom = new Pom();
    pom.addDependency("org.test", "rem-lib", "1.0");
    Assertions.assertEquals(1, pom.countDependencies());

    pom.rmDependency("org.test", "rem-lib");
    Assertions.assertEquals(0, pom.countDependencies(),
        "Dependency should be removed successfully");
  }

  /**
   * Tests the getDependencyFormatted() method returns non-empty
   * XML when dependencies are present.
   */
  @Test
  void testGetDependencyFormatted() {
    Pom pom = new Pom();
    pom.addDependency("com.example", "format-test", "1.1.1");
    String formatted = pom.getDependencyFormatted();

    Assertions.assertNotNull(formatted,
        "Should return a formatted XML string");
    Assertions.assertTrue(formatted.contains("<dependencies>"),
        "Result should contain <dependencies> tag");
    Assertions.assertTrue(formatted.contains("com.example"),
        "Result should contain the groupId");
    Assertions.assertTrue(formatted.contains("format-test"),
        "Result should contain the artifactId");
    Assertions.assertTrue(formatted.contains("1.1.1"),
        "Result should contain the version");
  }

  /**
   * Tests hasDependency(String, String) returns false if not found.
   */
  @Test
  void testHasDependencyNotPresent() {
    Pom pom = new Pom();
    pom.addDependency("com.example", "my-lib");
    boolean result = pom.hasDependency("com.foo", "bar");
    Assertions.assertFalse(result,
        "Should return false for non-existent dependency");
  }

  /**
   * Tests the overloaded hasDependency(Dependency) method
   * by adding a dependency object and confirming it is recognized.
   */
  @Test
  void testHasDependencyByObject() {
    Pom pom = new Pom();
    Dependency d = new Dependency("com.test", "obj-lib", "2.2.2");
    // Add the dependency via parameters
    pom.addDependency(d.getGroupId(), d.getArtifactId(), d.getVersion());

    Assertions.assertTrue(pom.hasDependency(d),
        "Should return true for the same dependency object");
  }

  /**
   * Tests countDependencies() accurately reflects the number of added deps.
   */
  @Test
  void testCountDependencies() {
    Pom pom = new Pom();
    pom.addDependency("group1:art1:1.0");
    pom.addDependency("group2:art2:2.0");

    Assertions.assertEquals(2, pom.countDependencies(),
        "Should have 2 dependencies in total");
  }

  /**
   * Tests getDependency(Dependency) retrieves the correct object.
   */
  @Test
  void testGetDependency() {
    Pom pom = new Pom();
    Dependency d = new Dependency("com.get", "sample", "9.9.9");
    pom.addDependency(d.getGroupId(), d.getArtifactId(), d.getVersion());

    Dependency found = pom.getDependency(d);
    Assertions.assertNotNull(found, "Should find the dependency object");
    Assertions.assertEquals("com.get", found.getGroupId());
    Assertions.assertEquals("sample", found.getArtifactId());
    Assertions.assertEquals("9.9.9", found.getVersion());
  }

  /**
   * Tests the toString() representation includes groupId/artifactId.
   */
  @Test
  void testToString() {
    Pom pom = new Pom();
    pom.addDependency("com.to", "string-lib", "1.0");
    String str = pom.toString();

    Assertions.assertTrue(str.contains("com.to:string-lib:1.0"),
        "toString() should include the new dependency");
  }
}
