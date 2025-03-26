package dev.buildcli.cli.commands.project;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.exceptions.CommandExecutorRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.buildcli.plugin.BuildCLIPlugin;
import dev.buildcli.plugin.enums.TemplateType;
import dev.buildcli.plugin.utils.BuildCLIPluginManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import static dev.buildcli.core.utils.input.InteractiveInputUtils.options;
import static dev.buildcli.core.utils.input.InteractiveInputUtils.question;


@Command(
    name = "init",
    aliases = {"i"},
    description = "Initializes a new project. This command sets up a new project structure.",
    mixinStandardHelpOptions = true
)
public class InitCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(InitCommand.class);

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

  private void createReadme(String projectName) throws IOException {
    File readme = new File("README.md");
    if (readme.createNewFile()) {
      try (FileWriter writer = new FileWriter(readme)) {
        writer.write("# " + projectName + "\n\nThis is the " + projectName + " project.");
      }
      logger.info("README.md file created.");
    }
  }

  private void createMainClass(String basePackage) throws IOException {
    String packagePath = "src/main/java/" + basePackage.replace('.', '/');
    File packageDir = new File(packagePath);
    if (!packageDir.exists() && !packageDir.mkdirs()) {
      throw new IOException("Could not create package directory: " + packagePath);
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
      logger.info("Main.java file created with package and basic content.");
    }
  }

  private void createPomFile(String projectName) throws IOException {
    File pomFile = new File("pom.xml");
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
      logger.info("pom.xml file created with default configuration.");
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

      String[] dirs = {
          "src/main/java/" + basePackage.replace('.', '/'),
          "src/main/resources",
          "src/test/java/" + basePackage.replace('.', '/')
      };

      for (String dir : dirs) {
        File directory = new File(dir);
        if (directory.mkdirs()) {
          logger.info("Directory created: {}", dir);
        }
      }

      try {
        createReadme(projectName);
        createMainClass(basePackage);
        createPomFile(projectName);
      } catch (IOException e) {
        throw new CommandExecutorRuntimeException(e);
      }

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
