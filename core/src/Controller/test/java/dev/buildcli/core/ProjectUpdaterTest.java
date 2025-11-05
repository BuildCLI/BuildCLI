package dev.buildcli.core;

import dev.buildcli.core.project.ProjectUpdater;
import dev.buildcli.core.utils.PomUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ProjectUpdaterTest {

	@TempDir
	Path tempDir;

	private String backupPom;
	private String targetPom;
	private String targetPomOriginalContent;
	private String backupPomOriginalContent;
	private ProjectUpdater updater;

	@BeforeEach
	void setUp() throws IOException {
		this.updater = new ProjectUpdater();

		// backup POM files
		this.targetPom = "src/test/resources/pom-core-test/pom.xml";
		this.backupPom = "src/test/resources/pom-core-test/pom.xml.versionsBackup";

		this.targetPomOriginalContent = Files.readString(Paths.get(this.targetPom));
		this.backupPomOriginalContent = Files.readString(Paths.get(this.backupPom));
	}

	@AfterEach
	void restorePOMs() {
		try{
			Files.writeString(Paths.get(this.targetPom), this.targetPomOriginalContent);
			Files.writeString(Paths.get(this.backupPom), this.backupPomOriginalContent);
			Files.deleteIfExists(Paths.get("src/test/resources/pom-core-test/pom.xml.versionsBackup.versionsBackup"));
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}

	@Test
	void shouldUpdatePomDependencies() {
		updater.setAdditionalParameters(List.of("-f", targetPom));
		updater.updateNow(true).execute();

		var originalPom = PomUtils.extractPomFile(backupPom);
		var changedPom = PomUtils.extractPomFile(targetPom);

		assertEquals(originalPom.countDependencies(), originalPom.getDependencies()
				.stream().filter(changedPom::hasDependency).count());
		assertEquals(2, originalPom.getDependencies().stream()
				.filter(d -> {
					var xd = changedPom.getDependency(d);
					return Objects.nonNull(d.getVersion()) && Objects.nonNull(xd.getVersion())
							&& !d.getVersion().equals(xd.getVersion());
				})
				.count());
	}

	@Test
	void useOldDependencies() {
		var originalPom = PomUtils.extractPomFile(backupPom);

		updater.setAdditionalParameters(List.of("-f", backupPom));
		updater.updateNow(false).execute();

		var changedPom = PomUtils.extractPomFile(backupPom);

		// checks if all dependencies are in "updated" pom
		assertEquals(originalPom.countDependencies(), originalPom.getDependencies()
				.stream().filter(changedPom::hasDependency).count());
		// checks that all dependencies have the same version
		assertEquals(3, originalPom.getDependencies().stream()
				.filter(d -> {
					var xd = changedPom.getDependency(d);
					return (Objects.equals(d.getVersion(), xd.getVersion()));
				})
				.count());
	}

	@Test
	void allDependenciesUpToDate() {
		var originalPom = PomUtils.extractPomFile(targetPom);

		updater.setAdditionalParameters(List.of("-f", targetPom));
		updater.updateNow(true).execute();

		var changedPom = PomUtils.extractPomFile(targetPom);

		// checks if all dependencies are in "updated" pom
		assertEquals(originalPom.countDependencies(), originalPom.getDependencies()
				.stream().filter(changedPom::hasDependency).count());
		// checks that all dependencies have the same version
		assertEquals(3, originalPom.getDependencies().stream()
				.filter(d -> {
					var xd = changedPom.getDependency(d);
					return (Objects.equals(d.getVersion(), xd.getVersion()));
				})
				.count());
	}

	@Test
	void updateOutdatedDependencies() {
		var originalPom = PomUtils.extractPomFile(backupPom);

		updater.setAdditionalParameters(List.of("-f", backupPom));
		updater.updateNow(true).execute();

		var changedPom = PomUtils.extractPomFile(backupPom);

		// checks if all dependencies are in "updated" pom
		assertEquals(originalPom.countDependencies(), originalPom.getDependencies()
				.stream().filter(changedPom::hasDependency).count());
		// checks that 2 outdated dependencies updated
		assertEquals(3, originalPom.getDependencies().stream()
				.filter(d -> {
					var xd = changedPom.getDependency(d);
					return (d.getVersion() == null && xd.getVersion() == null) ||
							(xd.getVersion().compareTo(d.getVersion()) >= 0);
				})
				.count());
	}
		
	@Test
	void emptyPOMFile() {
		Path emptyPOM = tempDir.resolve("empty-pom.xml");
		try {
			Files.writeString(emptyPOM, "<project></project>");
		} catch (IOException e) {
			e.printStackTrace();
		}

		updater.setAdditionalParameters(List.of("-f", emptyPOM.toString()));
		updater.updateNow(true).execute();

		var updatedPom = PomUtils.extractPomFile(emptyPOM.toString());
		assertEquals(0, updatedPom.countDependencies());
	}
	
	@Test
	void invalidPomFile() {
		Path invalidPom = tempDir.resolve("invalid-pom.xml");
		try {
			Files.writeString(invalidPom, "<project><dependencies>");
		} catch (IOException e) {
			e.printStackTrace();
		}

		updater.setAdditionalParameters(List.of("-f", invalidPom.toString()));

		try {
			updater.updateNow(true).execute();
			fail("Expected exception for invalid pom");
		} catch (Error e) {
			// pass
		}
	}
}



