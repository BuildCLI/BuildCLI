package org.buildcli.commands.project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.buildcli.core.ProjectInitializer;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.domain.git.GitCommandExecutor;
import org.buildcli.exceptions.CommandExecutorRuntimeException;
import org.buildcli.log.SystemOutLogger;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static org.buildcli.domain.git.GitCommands.*;


@Command(
    name = "init",
    aliases = {"i"},
    description = "Initializes a new project. This command sets up a new project structure.",
    mixinStandardHelpOptions = true
)
public class InitCommand implements BuildCLICommand {
  private static final Logger LOGGER = Logger.getLogger(ProjectInitializer.class.getName());
  private final GitCommandExecutor gitExecutor = new GitCommandExecutor();


  @Option(names = {"--name", "-n"}, defaultValue = "buildcli")
  private String projectName;
  @Option(names = {"--jdk", "-j"}, defaultValue = "17")
  private String jdkVersion;

  @Override
  public void run() {
    String basePackage = "org." + projectName.toLowerCase();

    if(isProjectAlreadyInitialized()){
      LOGGER.warning("A project already exists in this directory.");
      LOGGER.warning("Requesting user confirmation for overwrite.");

      String response = getUserInput("Do you want to create a backup before overwriting? (y/n): ");

      if(response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes")){
        createGitBackup();
        LOGGER.info("Backup completed. Proceeding with overwrite...");
      }
      LOGGER.info("Overwriting existing project...");
    }

    String[] dirs = {
        "src/main/java/" + basePackage.replace('.', '/'),
        "src/main/resources",
        "src/test/java/" + basePackage.replace('.', '/')
    };

    for (String dir : dirs) {
      File directory = new File(dir);
      if (directory.mkdirs()) {
        SystemOutLogger.log("Directory created: " + dir);
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

  private void createReadme(String projectName) throws IOException {
    File readme = new File("README.md");
    if (readme.createNewFile()) {
      try (FileWriter writer = new FileWriter(readme)) {
        writer.write("# " + projectName + "\n\nThis is the " + projectName + " project.");
      }
      SystemOutLogger.log("README.md file created.");
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
      SystemOutLogger.log("Main.java file created with package and basic content.");
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
      SystemOutLogger.log("pom.xml file created with default configuration.");
    }
  }

  private boolean isProjectAlreadyInitialized() {
    String[] projectFiles = {"pom.xml", "buildcli.config"};
    for (String fileName : projectFiles){
      if(new File(fileName).exists()){
        return true;
      }
    }
    return false;
  }

  private String getUserInput(String message) {
    LOGGER.warning(message);
    Scanner scanner = new Scanner(System.in);
    String input = scanner.nextLine();
    return input;
  }

  private void createGitBackup() {
    try {
      if (!gitExecutor.isGitRepository()) {
        LOGGER.info("Initializing Git repository...");
        gitExecutor.runGitCommandWithException(GIT, INIT);
      }

      LOGGER.info("Staging all files...");
      gitExecutor.runGitCommandWithException(GIT, ADD, ".");

      if (!gitExecutor.hasCommits()) {
        LOGGER.info("Creating initial commit...");
        gitExecutor.runGitCommandWithException(GIT, COMMIT, "-m", "Initial commit");
      }

      if (gitExecutor.hasChanges()) {
        LOGGER.info("Creating backup commit...");
        gitExecutor.runGitCommandWithException(GIT, COMMIT, "-m", "Backup before overwrite");
      } else {
        LOGGER.info("No changes to commit. Skipping backup commit.");
      }
    }catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to create Git backup!", e);
      throw new RuntimeException(e);
    }
  }
}
