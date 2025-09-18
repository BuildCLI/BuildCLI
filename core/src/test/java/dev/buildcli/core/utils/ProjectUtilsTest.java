package dev.buildcli.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectUtilsTest {

    @Mock
    private File mockDirectory;
    @Mock
    private File mockPomXml;
    @Mock
    private File mockBuildGradle;
    @Mock
    private File mockOtherFile;

    @Test
    @DisplayName("should return true when a valid project directory is provided")
    void isValid_ShouldReturnTrue_ForValidProjectDirectory() {
        // Arrange
        when(mockDirectory.exists()).thenReturn(true);
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.listFiles()).thenReturn(new File[]{mockOtherFile, mockPomXml});

        when(mockOtherFile.isFile()).thenReturn(true);
        when(mockOtherFile.getName()).thenReturn("README.md");
        when(mockPomXml.isFile()).thenReturn(true);
        when(mockPomXml.getName()).thenReturn("pom.xml");

        // Act
        boolean result = ProjectUtils.isValid(mockDirectory);

        // Assert
        assertTrue(result, "The method should return true for a valid project directory containing a pom.xml.");
    }

    @Test
    @DisplayName("should return true when a directory has a build.gradle file")
    void isValid_ShouldReturnTrue_ForDirectoryWithBuildGradle() {
        // Arrange
        when(mockDirectory.exists()).thenReturn(true);
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.listFiles()).thenReturn(new File[]{mockBuildGradle});

        when(mockBuildGradle.isFile()).thenReturn(true);
        when(mockBuildGradle.getName()).thenReturn("build.gradle");

        // Act
        boolean result = ProjectUtils.isValid(mockDirectory);

        // Assert
        assertTrue(result, "The method should return true for a directory containing a build.gradle.");
    }

    @Test
    @DisplayName("should return false when the directory does not exist")
    void isValid_ShouldReturnFalse_ForNonExistentDirectory() {
        // Arrange
        when(mockDirectory.exists()).thenReturn(false);

        // Act
        boolean result = ProjectUtils.isValid(mockDirectory);

        // Assert
        assertFalse(result, "The method should return false for a non-existent directory.");
    }

    @Test
    @DisplayName("should return false when the file is not a directory")
    void isValid_ShouldReturnFalse_ForNonDirectoryFile() {
        // Arrange
        when(mockDirectory.exists()).thenReturn(true);
        when(mockDirectory.isDirectory()).thenReturn(false);

        // Act
        boolean result = ProjectUtils.isValid(mockDirectory);

        // Assert
        assertFalse(result, "The method should return false when the provided file is not a directory.");
    }

    @Test
    @DisplayName("should return false when the directory is empty")
    void isValid_ShouldReturnFalse_ForEmptyDirectory() {
        // Arrange
        when(mockDirectory.exists()).thenReturn(true);
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.listFiles()).thenReturn(new File[]{});

        // Act
        boolean result = ProjectUtils.isValid(mockDirectory);

        // Assert
        assertFalse(result, "The method should return false for an empty directory.");
    }

    @Test
    @DisplayName("should return false when the directory contains no project files")
    void isValid_ShouldReturnFalse_ForDirectoryWithNoProjectFiles() {
        // Arrange
        when(mockDirectory.exists()).thenReturn(true);
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.listFiles()).thenReturn(new File[]{mockOtherFile});

        when(mockOtherFile.isFile()).thenReturn(true);
        when(mockOtherFile.getName()).thenReturn("README.md");

        // Act
        boolean result = ProjectUtils.isValid(mockDirectory);

        // Assert
        assertFalse(result, "The method should return false for a directory without project files.");
    }

    @Test
    @DisplayName("should return false when listFiles returns null")
    void isValid_ShouldReturnFalse_WhenListFilesReturnsNull() {
        // Arrange
        when(mockDirectory.exists()).thenReturn(true);
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.listFiles()).thenReturn(null);

        // Act
        boolean result = ProjectUtils.isValid(mockDirectory);

        // Assert
        assertFalse(result, "The method should return false when listFiles() returns null.");
    }
}