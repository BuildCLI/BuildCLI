package dev.buildcli.hooks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class HookLoader {
    private static final Logger log = LoggerFactory.getLogger(HookLoader.class);
    private final Gson gson = new Gson();
    private final String hooksFilePath;

    public HookLoader() {
        this.hooksFilePath = getGlobalHooksFilePath();
    }

    private String getGlobalHooksFilePath() {
        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.isFile()) {
                try (JarFile jar = new JarFile(jarFile)) {
                    String jarDir = jarFile.getParent();
                    return Paths.get(jarDir, "hooks.json").toString();
                }
            } else {
                String jarDir = jarFile.getParent();
                return Paths.get(jarDir, "hooks.json").toString();
            }
        } catch (URISyntaxException e) {
            log.error("URISyntaxException while determining JAR directory: {}", e.getMessage());
        } catch (IOException e) {
            log.error("IOException while determining JAR directory: {}", e.getMessage());
        }
        return null;
    }

    public Set<Hook> loadHooks() {
        log.debug("Loading hooks from JSON file: {}", hooksFilePath);
        File file = new File(hooksFilePath);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write("[]");
                    }
                }
            } catch (IOException e) {
                log.error("Error loading hooks file: \n{}", e.getMessage());
            }
        }

        Type type = new TypeToken<Set<Hook>>() {}.getType();
        try (FileReader reader = new FileReader(file)) {
            Set<Hook> loadedHooks = gson.fromJson(reader, type);
            return loadedHooks != null ? loadedHooks : new HashSet<>();
        } catch (IOException e) {
            log.warn("Error loading hooks: \n{}", e.getMessage());
            return new HashSet<>();
        }
    }

    public void saveHooks(Set<Hook> hooks) {
        log.info("Saving hook to JSON file: {}", hooksFilePath);
        File file = new File(hooksFilePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            var created = parent.mkdirs();
            log.info("Created a json file at {} {}", parent,created);
        }
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(hooks, writer);
            log.info("Hook saved successfully.");
        } catch (IOException e) {
            log.error("Error saving hooks: {}", e.getMessage());
        }
    }
}