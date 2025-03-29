package dev.buildcli.core.project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Deprecated(forRemoval = true)
public class ProjectInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ProjectInitializer.class);

    public void initializeProject(String projectName) throws IOException {
        String baseProject = (projectName != null && !projectName.isBlank()) ? projectName : "buildcli";
        String basePackage = "org." + baseProject.toLowerCase();
        String[] dirs = {
                "src/main/java/" + basePackage.replace('.', '/'),
                "src/main/resources",
                "src/test/java/" + basePackage.replace('.', '/')
        };

        for (String dir : dirs) {
            File directory = new File(dir);
            if (directory.mkdirs()) {
                logger.info("Directory created: {}",dir);
            }
        }

        createReadme(baseProject);
        createMainClass(basePackage);
        createPomFile(baseProject);
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
                            <maven.compiler.source>17</maven.compiler.source>
                            <maven.compiler.target>17</maven.compiler.target>
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
                """.formatted(projectName.toLowerCase(), projectName, projectName.toLowerCase()));
            }
            logger.info("pom.xml file created with default configuration.");
        }
    }

    /**
     * Creates a configuration file for the specified profile with predefined content.
     *
     * @param profile the name of the profile (e.g., dev, prod, test)
     */
    public static void createProfileConfig(String profile) {
        String fileName = "src/main/resources/application-" + profile + ".properties";
        File profileFile = new File(fileName);

        String content = """
                # Configurações do perfil de %s
                app.name=BuildCLI %s Environment
                app.message=Running in %s Mode
                logging.level=DEBUG
                """.formatted(profile, profile, profile);

        try {
            if (profileFile.createNewFile()) {
                try (FileWriter writer = new FileWriter(profileFile)) {
                    writer.write(content);
                }
                logger.info("Configuration profile created: {}", fileName);
            } else {
                logger.warn("Configuration profile already exists: {}", fileName);
            }
        } catch (IOException e) {
           logger.error("Failed to create or write configuration profile: {}", fileName, e);
        }
    }
}
