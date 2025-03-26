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
        public class %sCommand extends BuildCLICommandPlugin {
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
      
        <properties>
          <java.version>21</java.version>
          <maven.compiler.target>21</maven.compiler.target>
          <maven.compiler.source>21</maven.compiler.source>
        </properties>
      
        <dependencies>
          <dependency>
            <groupId>dev.buildcli</groupId>
            <artifactId>buildcli-plugin</artifactId>
            <version>0.14.0</version>
            <scope>provided</scope>
          </dependency>
          <dependency>
            <groupId>dev.buildcli</groupId>
            <artifactId>buildcli-core</artifactId>
            <version>0.14.0</version>
            <scope>provided</scope>
          </dependency>
          <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
            <version>4.7.6</version>
            <scope>provided</scope>
          </dependency>
        </dependencies>
      
        <build>
          <finalName>%s</finalName>
          <plugins>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-assembly-plugin</artifactId>
              <configuration>
                <descriptorRefs>
                  <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
                <appendAssemblyId>false</appendAssemblyId>
                <finalName>%s</finalName>
              </configuration>
              <executions>
                <execution>
                  <id>make-assembly</id>
                  <phase>package</phase>
                  <goals>
                    <goal>single</goal>
                  </goals>
                </execution>
              </executions>
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
    String normalizedName = pluginName.replaceAll("[^a-zA-Z0-9]", "").trim();
    String pluginPackage = normalizedName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
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
    if (!Files.exists(javaFilePath.getParent())) {
      Files.createDirectories(javaFilePath.getParent());
    }
    Files.writeString(javaFilePath, commandContent);

    // Write service provider file
    var serviceFilePath = Paths.get(
        rootDirectory.getAbsolutePath(),
        "src/main/resources/META-INF/services",
        SERVICE_FILE
    );
    if (!Files.exists(serviceFilePath.getParent())) {
      Files.createDirectories(serviceFilePath.getParent());
    }
    String serviceContent = String.format("dev.buildcli.plugin.%s.%sCommand", pluginPackage, className);
    Files.writeString(serviceFilePath, serviceContent);

    //Write pf4j file
    var pf4jFilePath = Paths.get(rootDirectory.getAbsolutePath(),
        "src/main/resources",
        "plugin.properties"
    );
    if (!Files.exists(pf4jFilePath.getParent())) {
      Files.createDirectories(pf4jFilePath.getParent());
    }
    var pf4jContent = String.format("""
        plugin.id=%s
        plugin.version=0.0.1-SNAPSHOT
        plugin.class=dev.buildcli.plugin.%s.%sCommand
        """, className.toLowerCase(), pluginPackage, className);

    Files.writeString(pf4jFilePath, pf4jContent);

    // Write POM file
    Path pomFilePath = Paths.get(rootDirectory.getAbsolutePath(), "pom.xml");
    if (!Files.exists(pomFilePath.getParent())) {
      Files.createDirectories(pomFilePath.getParent());
    }
    String pomContent = String.format(POM_TEMPLATE, pluginPackage, pluginPackage, pluginPackage);
    Files.writeString(pomFilePath, pomContent);
  }

  private String capitalizeFirstLetter(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }
}