package dev.buildcli.plugin.builders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CommandPluginBuilder implements PluginBuilder {
  private static final String[] PROJECT_DIRECTORIES = {
      "src/main/java/dev/buildcli/plugin",
      "src/main/resources",
      "src/main/resources/META-INF/services",
      "src/test/java/dev/buildcli/plugin",
      "src/test/resources"
  };

  private static final String COMMAND_TEMPLATE = """
        package dev.buildcli.plugin.%s;
        
        import dev.buildcli.plugin.BuildCLICommandPlugin;
        import picocli.CommandLine.Command;
        
        @Command(name = "%s", mixinStandardHelpOptions = true)
        public class %sCommand implements BuildCLICommandPlugin {
          @Override
          public void run() {
            // Plugin implementation
            System.out.println("Hello World, %s!");
          }
        
          @Override
          public String version() {
            return "0.0.1-SNAPSHOT";
          }
        
          @Override
          public String name() {
            return "%s";
          }
        
          @Override
          public String description() {
            return "Build CLI Plugin";
          }
        
          @Override
          public String[] parents() {
            return null;
          }
        }
        """;

  private static final String POM_TEMPLATE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>
          <groupId>dev.buildcli.plugin</groupId>
          <artifactId>buildcli-plugin-%s</artifactId>
          <version>0.0.1-SNAPSHOT</version>
        
          <dependencies>
            <dependency>
              <groupId>dev.buildcli</groupId>
              <artifactId>buildcli-plugin</artifactId>
              <version>0.14.0</version>
            </dependency>
            <dependency>
              <groupId>dev.buildcli</groupId>
              <artifactId>buildcli-core</artifactId>
              <version>0.14.0</version>
            </dependency>
            <dependency>
              <groupId>info.picocli</groupId>
              <artifactId>picocli</artifactId>
              <version>4.7.6</version>
            </dependency>
          </dependencies>
        
          <build>
            <finalName>%s</finalName>
            <plugins>
              <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
              </plugin>
            </plugins>
           </build>
        </project>
        """;

  private static final String SERVICE_FILE = "dev.buildcli.plugin.BuildCLICommandPlugin";

  @Override
  public File build(File directory, String name) {
    try {
      // Create project structure
      createProjectStructure(directory, name);

      // Generate and write files
      writeProjectFiles(directory, name);

      return directory;
    } catch (IOException e) {
      throw new RuntimeException("Failed to build command plugin: " + e.getMessage(), e);
    }
  }

  private void createProjectStructure(File rootDirectory, String pluginName) {
    String pluginPackage = pluginName.toLowerCase().trim();

    Arrays.stream(PROJECT_DIRECTORIES).forEach(dirPath -> {
      File targetDir;
      if (dirPath.contains("java")) {
        targetDir = new File(rootDirectory, dirPath + "/" + pluginPackage);
      } else {
        targetDir = new File(rootDirectory, dirPath);
      }

      if (!targetDir.mkdirs() && !targetDir.exists()) {
        throw new RuntimeException("Failed to create directory: " + targetDir.getAbsolutePath());
      }
    });
  }

  private void writeProjectFiles(File rootDirectory, String pluginName) throws IOException {
    String normalizedName = pluginName.trim();
    String pluginPackage = normalizedName.toLowerCase();
    String className = capitalizeFirstLetter(normalizedName);

    // Write Java class file
    String commandContent = String.format(
        COMMAND_TEMPLATE,
        pluginPackage, pluginPackage, className, pluginPackage, pluginPackage
    );

    Path javaFilePath = Paths.get(
        rootDirectory.getAbsolutePath(),
        "src/main/java/dev/buildcli/plugin",
        pluginPackage,
        className + "Command.java"
    );
    Files.writeString(javaFilePath, commandContent);

    // Write service provider file
    Path serviceFilePath = Paths.get(
        rootDirectory.getAbsolutePath(),
        "src/main/resources/META-INF/services",
        SERVICE_FILE
    );
    String serviceContent = String.format("dev.buildcli.plugin.%s.%sCommand", pluginPackage, className);
    Files.writeString(serviceFilePath, serviceContent);

    // Write POM file
    Path pomFilePath = Paths.get(rootDirectory.getAbsolutePath(), "pom.xml");
    String pomContent = String.format(POM_TEMPLATE, pluginPackage, pluginPackage);
    Files.writeString(pomFilePath, pomContent);
  }

  private String capitalizeFirstLetter(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }
}