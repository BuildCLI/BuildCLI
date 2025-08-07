package dev.buildcli.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

public class DefaultManifestReader implements ManifestReader {

    @Override
    public Manifest readManifest(InputStream inputStream) {
        return readManifestWithThrows(inputStream);
    }

    private Manifest readManifestWithThrows(InputStream inputStream) {
        try {
            return new Manifest(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to read the content of the MANIFEST.MF file", e);
        }
    }
}
