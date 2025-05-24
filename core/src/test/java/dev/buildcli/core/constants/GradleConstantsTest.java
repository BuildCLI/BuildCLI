package dev.buildcli.core.constants;

import dev.buildcli.core.utils.OS;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class GradleConstantsTest {
    @Test
    void testGradleCmd() {
        String expectedGradleWindows = "gradle.bat";
        String expectedGradleUnix = "gradle";

        assertEquals(expectedGradleWindows, GradleConstants.GRADLE_CMD_WINDOWS);
        assertEquals(expectedGradleUnix, GradleConstants.GRADLE_CMD_UNIX);
        

        if(OS.isWindows()){
            assertEquals(expectedGradleWindows, GradleConstants.GRADLE_CMD);
        } else {
            assertEquals(expectedGradleUnix, GradleConstants.GRADLE_CMD);
        }
    }

    @Test
    void testBuildFile() {
        String expectedFile = "build.gradle";
        assertEquals(expectedFile, GradleConstants.FILE);
    }

    @Test 
    void testDependenciesPattern() {
        String expectedPattern = "##dependencies##";
        assertEquals(expectedPattern, GradleConstants.DEPENDENCIES_PATTERN);
    }


    @Test
    void testBuildDirectory() {
        String expectedBuildDir = "build";
        assertEquals(expectedBuildDir, GradleConstants.BUILD_DIR);
    }
}
