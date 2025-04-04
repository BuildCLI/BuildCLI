package dev.buildcli.cli.commands.project.add;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.exceptions.DockerException;
import dev.buildcli.core.model.DockerComposeConfig;
import dev.buildcli.core.utils.DockerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command( name = "docker-compose",
    aliases =  {"dc"},
    description = "Manager Docker Compose configurations and lifecycle.",
    mixinStandardHelpOptions = true
)
public class DockerComposeCommand implements BuildCLICommand {

    private static final Logger logger = LoggerFactory.getLogger(DockerComposeCommand.class.getName());

    @Option(names = {"--port", "-p"},
            description = "Port mappings (ex: 8080:8080)")
    private List<String> ports;

    @Option(names = {"--volume", "-v"},
            description = "Volume mapping (ex: ./data:/app/data)")
    private List<String> volumes;

    @Option(names = {"--cpu", "-c"},
            description = "CPU limit (ex: 2)")
    private String cpu;

    @Option(names = {"--memory", "-m"},
            description = "Memory limit (ex: 512m, 2g)")
    private String memory;

    @Option(names = {"--dockerfile", "-d"},
            description = "Path of DockerFile (default: ./DockerFile)")
    private String dockerFilePath;

    public void setDockerFilePath(String dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
    }

    @Override
    public void run() {

        DockerComposeConfig config = new DockerComposeConfig(ports, volumes, cpu, memory, dockerFilePath);
        try {
            DockerManager.setupDockerCompose(config);
            logger.info("docker-compose.yml created successfully!");
        } catch (DockerException | CommandLine.ExecutionException e) {
            throw new CommandLine.ExecutionException(new CommandLine(this), "Failed to setup docker-compose: " + e.getMessage(), e);
        }
    }
}
