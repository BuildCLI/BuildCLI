package dev.buildcli.core.constants;

import dev.buildcli.core.utils.OS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MavenConstantsTest {

    @Test
    void testMavenCmdConstants() {
        if (OS.isWindows()) {
            assertEquals("mvn.cmd", MavenConstants.MAVEN_CMD);
        } else {
            assertEquals("mvn", MavenConstants.MAVEN_CMD);
        }
    }

    @Test
    void testPomFileConstant() {
        assertEquals("pom.xml", MavenConstants.FILE);
    }

    @Test
    void testDependenciesPatternConstant() {
        assertEquals("##dependencies##", MavenConstants.DEPENDENCIES_PATTERN);
    }

    @Test
    void testTargetDirectoryConstant() {
        assertEquals("target", MavenConstants.TARGET);
    }
}

