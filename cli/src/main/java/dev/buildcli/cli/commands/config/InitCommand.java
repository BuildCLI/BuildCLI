package dev.buildcli.cli.commands.project;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.exceptions.CommandExecutorRuntimeException;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.plugin.BuildCLIPlugin;
import dev.buildcli.plugin.enums.TemplateType;
import dev.buildcli.plugin.utils.BuildCLIPluginManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.options;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.question;

@Command(
    name = "init",
    aliases = {"i"},
    description = "Initializes a new project. This command sets up a new project structure.",
    mixinStandardHelpOptions = true
)
public class InitCommand implements BuildCLICommand {
  @Option(names = {"--name", "-n"}, defaultValue = "buildcli")
  private String projectName;

  @Option(names = {"--jdk", "-j"}, defaultValue = "17")
  private String jdkVersion;

  @Option(names = {"--template", "-t"}, description = "Choose project initializr by available templates", defaultValue = "false")
  private boolean template;

  @Override
  public void run() {
    if (template) {
      var templates = BuildCLIPluginManager.getTemplatesByType(TemplateType.PROJECT);
      templates = new LinkedList<>(templates);
      templates.add(new QuickStartProject());

      var chooseTemplate = options("Choose a project template", templates, BuildCLIPlugin::name);
      chooseTemplate.execute();
    } else {
      new QuickStartProject().execute();
    }
  }

  private void createReadme(File root, String projectName) throws IOException {
    File readme = new File(root, "README.md");
    if (readme.createNewFile()) {
      try (FileWriter writer = new FileWriter(readme)) {
        writer.write("# " + projectName + "\n\nThis is the " + projectName + " project.");
      }
      SystemOutLogger.log("README.md file created.");
    }
  }

  private void createMainClass(File root, String basePackage) throws IOException {
    String packagePath = "src/main/java/" + basePackage.replace('.', '/');
    File packageDir = new File(root, packagePath);
    if (!packageDir.exists() && !packageDir.mkdirs()) {
      throw new IOException("Could not create package directory: " + packageDir.getPath());
    }

    File javaClass = new File(packageDir, "Main.java");
    if (javaClass.createNewFile()) {
      try (FileWriter writer = new FileWriter(javaClass)) {
        writer.write("""
                package %s;

                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
            """.formatted(basePackage));
      }
      SystemOutLogger.log("Main.java file created with package and basic content.");
    }
  }

  private void createPomFile(File root, String projectName, String jdkVersion) throws IOException {
    File pomFile = new File(root, "pom.xml");
    if (pomFile.createNewFile()) {
      try (FileWriter writer = new FileWriter(pomFile)) {
        writer.write("""
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://www.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0-SNAPSHOT</version>

                    <properties>
                        <maven.compiler.source>%s</maven.compiler.source>
                        <maven.compiler.target>${maven.compiler.source}</maven.compiler.target>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>

                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-engine</artifactId>
                            <version>5.8.1</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>

                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.8.1</version>
                                <configuration>
                                    <source>${maven.compiler.source}</source>
                                    <target>${maven.compiler.target}</target>
                                </configuration>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-jar-plugin</artifactId>
                                <version>3.2.0</version>
                                <configuration>
                                    <archive>
                                        <manifest>
                                            <mainClass>org.%s.Main</mainClass>
                                        </manifest>
                                    </archive>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
            """.formatted(projectName.toLowerCase(), projectName, jdkVersion, projectName.toLowerCase()));
      }
      SystemOutLogger.log("pom.xml file created with default configuration.");
    }
  }

  private class QuickStartProject extends dev.buildcli.plugin.BuildCLITemplatePlugin {
    @Override
    public TemplateType type() {
      return TemplateType.PROJECT;
    }

    @Override
    public void execute() {
      projectName = projectName == null || projectName.isEmpty() ? question("Enter project name") : projectName;
      jdkVersion = jdkVersion == null || jdkVersion.isEmpty() ? question("Enter jdk version") : jdkVersion;
      var basePackage = question("Enter base-package");

      File currentDir = new File(System.getProperty("user.dir"));
      File projectRoot = new File(currentDir, projectName);

      if (!projectRoot.exists() && !projectRoot.mkdirs()) {
        throw new CommandExecutorRuntimeException("Failed to create project root directory: " + projectRoot.getPath());
      }

      String[] dirs = {
          "src/main/java/" + basePackage.replace('.', '/'),
          "src/main/resources",
          "src/test/java/" + basePackage.replace('.', '/')
      };

      for (String dir : dirs) {
        File directory = new File(projectRoot, dir);
        if (directory.mkdirs()) {
          SystemOutLogger.log("Directory created: " + directory.getPath());
        }
      }

      try {
        createReadme(projectRoot, projectName);
        createMainClass(projectRoot, basePackage);
        createPomFile(projectRoot, projectName, jdkVersion);
      } catch (IOException e) {
        throw new CommandExecutorRuntimeException(e);
      }

      SystemOutLogger.log("Project '%s' initialized successfully in: %s"
          .formatted(projectName, projectRoot.getAbsolutePath()));
    }

    @Override
    public String name() {
      return "quickstart";
    }

    @Override
    public String description() {
      return "Quick start project";
    }

    @Override
    public String version() {
      return "0.0.1";
    }
  }
}
