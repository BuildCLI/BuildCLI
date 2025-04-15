package dev.buildcli.core;

import dev.buildcli.core.project.ProjectUpdater;
import dev.buildcli.core.utils.PomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectUpdaterTest {

	@TempDir
	Path tempDir;

	private static String backupPom;
	private static String targetPom;
	private ProjectUpdater updater;

	@BeforeEach
	void setUp() throws IOException {
		this.updater = new ProjectUpdater();

		InputStream originPomStream = getClass().getResourceAsStream("/pom-core-test/pom.xml");

		Path tempPom = tempDir.resolve("pom.xml");

		Files.copy(originPomStream, tempPom, StandardCopyOption.REPLACE_EXISTING);

		targetPom = tempPom.toString();
		backupPom = targetPom + ".versionsBackup";

	}

	@Test
	void shouldUpdatePomDependencies() {

		targetPom = "src/test/resources/pom-core-test/pom.xml";
		backupPom = "src/test/resources/pom-core-test/pom.xml.versionsBackup";

		this.updater.setAdditionalParameters(List.of("-f", targetPom));
		this.updater.updateNow(true).execute();

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

}
