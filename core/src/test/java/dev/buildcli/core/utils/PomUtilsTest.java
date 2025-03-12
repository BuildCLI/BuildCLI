package dev.buildcli.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PomUtilsTest {

	@Test
	void shouldRemoveExistingDependency() {
		var groupId = "info.picocli";
		var artifactId = "picocli";
		var changedPom = PomUtils.rmDependencyToPom("src/test/resources/pom-utils-test/pom.xml",
				new String[]{ groupId.concat(":").concat(artifactId) });
		assertFalse(changedPom.hasDependency(groupId, artifactId));
		assertFalse(changedPom.hasDependency("org.junit", "junit-bom"));
		assertEquals(2, changedPom.countDependencies());
	}
	
	@Test
	void shouldNotRemoveNonExistentDependency() {
		var groupId = "org.hibernate";
		var artifactId = "hibernate-core";
		var changedPom = PomUtils.rmDependencyToPom("src/test/resources/pom-utils-test/pom.xml", 
				new String[]{ groupId.concat(":").concat(artifactId) });
		assertFalse(changedPom.hasDependency(groupId, artifactId));
		assertEquals(3, changedPom.countDependencies());
	}
	
	@Test
	void shouldAddNonExistentDependency() {
		var groupId = "org.hibernate";
		var artifactId = "hibernate-core";
		var changedPom = PomUtils.addDependencyToPom("src/test/resources/pom-utils-test/pom.xml", 
				new String[]{ groupId.concat(":").concat(artifactId) });
		assertTrue(changedPom.hasDependency(groupId, artifactId));
		assertEquals(4, changedPom.countDependencies());
	}
	
	@Test
	void shouldAddDependencyToNonDependenciesPom() {
		var groupId = "org.hibernate";
		var artifactId = "hibernate-core";
		var changedPom = PomUtils.addDependencyToPom("src/test/resources/pom-utils-test/non-dependencies-pom.xml", 
				new String[]{ groupId.concat(":").concat(artifactId) });
		assertTrue(changedPom.hasDependency(groupId, artifactId));
		assertEquals(1, changedPom.countDependencies());
	}
}
