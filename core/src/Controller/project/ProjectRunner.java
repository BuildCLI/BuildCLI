package dev.buildcli.core.project;

import dev.buildcli.core.utils.ProfileManager;
import dev.buildcli.core.utils.SystemCommands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated(forRemoval = true)
public class ProjectRunner {
    private static final Logger logger = Logger.getLogger(ProjectRunner.class.getName());
    private final ProfileManager profileManager;

    public ProjectRunner() {
        this.profileManager = new ProfileManager();
    }

    public void runProject() {
        try {
            // Carregar o perfil ativo
            String activeProfile = profileManager.getActiveProfile();
            if (activeProfile == null) {
                logger.warning("No active profile set. Using default profile.");
                activeProfile = "default";
            }

            // Carregar as propriedades do perfil ativo
            Properties properties = loadProfileProperties(activeProfile);
            String profileMessage = properties.getProperty("app.message", "Running with no specific profile");

            // Exibir a mensagem do perfil ativo no console
            System.out.println("Active Profile: " + activeProfile);
            System.out.println(profileMessage);

            // Compilar e executar o projeto
            compileProject(); // Garante que o projeto está compilado
            runJar(); // Executa o JAR gerado
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to run project", e);
            Thread.currentThread().interrupt();
        }
    }

    private void compileProject() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                SystemCommands.MVN.getCommand(),
                "package",
                "-q" // Modo silencioso
        );
        builder.inheritIO();
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to compile project. Maven exited with code " + exitCode);
        }
        System.out.println("Project compiled successfully.");
    }

    private void runJar() throws IOException, InterruptedException {
        File targetDir = new File("target");
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new IOException("Target directory does not exist or is not a directory.");
        }

        // Busca pelo arquivo JAR na pasta target
        File[] jarFiles = targetDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            throw new IOException("No JAR file found in target directory.");
        }

        // Assume que o primeiro arquivo JAR encontrado é o correto
        File jarFile = jarFiles[0];
        String jarPath = jarFile.getAbsolutePath();

        // Executa o arquivo JAR
        ProcessBuilder builder = new ProcessBuilder(
                "java",
                "-jar",
                jarPath
        );
        builder.inheritIO();
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to run project JAR. Process exited with code " + exitCode);
        }
    }


    private Properties loadProfileProperties(String profile) {
        Properties properties = new Properties();
        String propertiesFile = "src/main/resources/application-" + profile + ".properties";
        Path propertiesFilePath = Paths.get(propertiesFile);
        String messageWarning = "Profile properties file not found: " + propertiesFile + ". Continuing with empty properties.";

        if (!Files.exists(propertiesFilePath)) {
            logger.warning(messageWarning);
            return properties;
        }
        try (InputStream input = Files.newInputStream(Paths.get(propertiesFile))) {
            properties.load(input);
        } catch (IOException e) {
            logger.warning(messageWarning);
        }
        return properties;
    }
}
