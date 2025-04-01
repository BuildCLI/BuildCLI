package dev.buildcli.cli.commands.run;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.DockerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "up",
        description = "start all containers")
public class OrchestrationUpCommand implements BuildCLICommand {

    private static final Logger logger = LoggerFactory.getLogger(OrchestrationUpCommand.class.getName());
    private final DockerManager dockerManager;

    @Option(names = {"--build", "-b"}, description = "Force image reconstruction")
    private boolean rebuild;

    public OrchestrationUpCommand(DockerManager dockerManager) {
        this.dockerManager = dockerManager;
    }

    public OrchestrationUpCommand() {
        this.dockerManager = new DockerManager();
    }

    @Override
    public void run() {
        try {
            dockerManager.upContainer(rebuild);
            String message = "All containers have been successfully started.";
            logger.info(message);
        } catch (Exception e) {
            throw new CommandLine.ExecutionException(new CommandLine(this), "Failed to start containers: " + e.getMessage(), e);
        }
    }
}
