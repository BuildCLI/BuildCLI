package dev.buildcli.cli.core;

import dev.buildcli.core.utils.DefaultManifestReader;
import dev.buildcli.core.utils.ManifestReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultManifestReaderTest {

    @Test
    @DisplayName("Read valid manifest")
    void testReadManifest_success() {
        // given
        String[] manifestContents = {
                "Build-Directory: /build/test/output\n",
                "Manifest-Version: 1.0\n" + "Build-Directory: /build/test/output\n",
        };

        for (String manifestContent : manifestContents) {
            InputStream inputStream = new ByteArrayInputStream(manifestContent.getBytes());
            ManifestReader reader = new DefaultManifestReader();

            // when
            Manifest manifest = reader.readManifest(inputStream);

            // then
            String value = manifest.getMainAttributes().getValue("Build-Directory");
            assertEquals("/build/test/output", value, "input [" + manifestContent + "]");
        }
    }

    @Test
    @DisplayName("Read manifest without 'Build-Directory'")
    void testReadManifest_success_empty() {
        // given
        String[] manifestContents = {
                "Manifest-Version: 1.0\n",
        };

        for (String manifestContent : manifestContents) {
            InputStream inputStream = new ByteArrayInputStream(manifestContent.getBytes());
            ManifestReader reader = new DefaultManifestReader();

            // when
            Manifest manifest = reader.readManifest(inputStream);

            // then
            String buildDirectory = manifest.getMainAttributes().getValue("Build-Directory");
            assertEquals(null, buildDirectory, "input: [" + manifestContent + "]");
        }
    }

    @Test
    @DisplayName("Fail on invalid manifest")
    void testReadManifest_fail_read() {
        // given
        String[] manifestContents = {
                "Build-Directory\n",
                "Build-Directory /build/test/output\n",
                "Build-Directory:\n  continued-line-without-space",
                "Build-Directory:\ncontinued"
        };

        for (String manifestContent : manifestContents) {
            InputStream inputStream = new ByteArrayInputStream(manifestContent.getBytes());
            ManifestReader reader = new DefaultManifestReader();

            // when & then
            assertThrows(RuntimeException.class, () -> {
                reader.readManifest(inputStream);
            }, "input: [" + manifestContent + "]");
        }
    }
}
