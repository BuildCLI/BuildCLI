package dev.buildcli.core.utils;

import java.io.InputStream;
import java.util.jar.Manifest;

public interface ManifestReader {

    Manifest readManifest(InputStream inputStream);
}
