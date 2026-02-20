package dev.buildcli.cli.commands.ops.add;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.docker.compose.*;
import dev.buildcli.core.domain.docker.file.*;
import dev.buildcli.core.utils.ConditionalRunner;
import dev.buildcli.core.utils.docker.DockerHubUtils;
import dev.buildcli.core.utils.docker.DockerHubUtils.DockerImage;
import dev.buildcli.plugin.BuildCLIPlugin;
import dev.buildcli.plugin.enums.TemplateType;
import dev.buildcli.plugin.utils.BuildCLIPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static dev.buildcli.core.domain.docker.compose.NetworkDriver.BRIDGE;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.*;
import static dev.buildcli.core.utils.docker.DockerHubUtils.searchImagesWithTags;
import static java.util.Optional.ofNullable;

@Command(name = "docker", aliases = {"d"}, description = "Generates a Dockerfile for the project. "
    + "Alias: 'docker' and 'df'. Allows customizing the base image, exposed ports, and file name.",
    mixinStandardHelpOptions = true)
public class DockerCommand implements BuildCLICommand {
  private final Logger logger = LoggerFactory.getLogger(DockerCommand.class.getName());

  @Option(names = {"--force"}, description = "Use to overwrite existing dockerfile specified by name option.", defaultValue = "false")
  private Boolean force;

  @Option(names = {"--template", "-t"}, description = "Docker template, internal or plugin", defaultValue = "false")
  private boolean template;

  @Option(names = {"--output", "-o"}, description = "Directory output path", defaultValue = ".")
  private File output;

  @Override
  public void run() {

    if (template) {
      handleTemplate();
    } else {
      handleDockerGeneration();
    }
  }

  private void handleTemplate() {
    var templates = BuildCLIPluginManager.getTemplatesByType(TemplateType.DOCKER);
    if (templates.isEmpty()) {
      return;
    }

    var dockerTemplate = options("Choose a Docker template", templates, BuildCLIPlugin::name);
    if (dockerTemplate == null) {
      return;
    }

    dockerTemplate.execute();
  }

  private void handleDockerGeneration() {
    var dockerType = options("Choose a Docker type", List.of("Dockerfile", "Docker Compose"));
    switch (dockerType) {
      case "Dockerfile" -> generateDockerfile();
      case "Docker Compose" -> generateDockerCompose();
      default -> logger.error("Error: Invalid Docker type.");
    }
  }

  private void generateDockerCompose() {
    var dockerComposeFileName = question("Docker Compose file name", false);
    if (!checkFileOverwrite(dockerComposeFileName)) {
      return;
    }
    Map<String, Network> networks = null;
    Map<String, Volume> volumes = null;

    var version = question("Docker Compose version", false);
    var services = createServices();

    networks = ConditionalRunner.runOnCondition(confirm("Do you want to add networks?"), this::createNetworks).orElse(null);

    volumes = ConditionalRunner.runOnCondition(confirm("Do you want to add volumes?"), this::createVolumes).orElse(null);

    var dockerCompose = new DockerCompose(dockerComposeFileName, version, services, networks, volumes);
    writeToFile(dockerComposeFileName, dockerCompose.toString());
  }

  private boolean checkFileOverwrite(String fileName) {
    var file = new File(output, fileName);
    if (file.exists() && !force) {
      System.out.println("Docker Compose file already exists.");
      return false;
    }
    return true;
  }

  private Map<String, Service> createServices() {
    var services = new LinkedHashMap<String, Service>();
    do {
      var serviceName = question("Service name", false);
      var service = createService(serviceName, services.keySet());
      services.put(serviceName, service);
    } while (confirm("Add another service?"));
    return services;
  }

  private Service createService(String serviceName, Set<String> existingServices) {
    var isUsingImage = confirm("Do you want to use image?");
    String dockerfile = null;
    String imageName = null;

    if (isUsingImage) {
      imageName = selectDockerImage();
    } else {
      dockerfile = question("Dockerfile name", false);
    }

    var ports = ConditionalRunner.runOnCondition(confirm("Do you want add port mapping?"), this::collectPorts).orElse(null);
    var commands = ConditionalRunner.runOnCondition(confirm("Do you want add command?"), this::collectCommands).orElse(null);
    var dependsOn = ConditionalRunner.runOnCondition(confirm("Do you want add depends on?"), () -> collectDependencies(existingServices)).orElse(null);
    var environments = ConditionalRunner.runOnCondition(confirm("Do you want add envs?"), this::collectEnvironmentVariables).orElse(null);
    var volumes = ConditionalRunner.runOnCondition(confirm("Do you want add volumes?"), this::collectVolumes).orElse(null);
    var networks = ConditionalRunner.runOnCondition(confirm("Do you want add networks?"), this::collectNetworks).orElse(null);
    var restartPolicy = ConditionalRunner.runOnCondition(confirm("Do you want add restart policy?"), this::createRestartPolicy).orElse(null);
    var labels = ConditionalRunner.runOnCondition(confirm("Do you want add labels?"), this::collectLabels).orElse(null);
    var healthcheck = ConditionalRunner.runOnCondition(confirm("Do you want add healthcheck?"), this::createHealthcheck).orElse(null);

    return new Service(
        imageName,
        dockerfile,
        commands,
        dependsOn,
        environments,
        ports,
        volumes,
        networks,
        restartPolicy,
        labels,
        healthcheck
    );
  }

  private RestartPolicy createRestartPolicy() {
    return options("Restart policy", List.of(RestartPolicy.values()), s -> s.name().toLowerCase());
  }

  private String selectDockerImage() {
    var imageName = question("Image name", true);
    return ConditionalRunner.runOnCondition(confirm("Do you want search in DockerHub?"), () -> {
      var images = DockerHubUtils.searchImagesWithTags(imageName);
      var image = options("Choose an image", images);
      return image != null ? image.toString() : null;
    }).orElse(imageName.matches("^.+:[A-Za-z0-9]+$") ? imageName : imageName + ":latest");
  }

  private List<String> collectPorts() {
    var ports = new ArrayList<String>();
    do {
      var port = question("Port", false);
      if (port == null || !port.matches("\\d{2,6}")) break;
      ports.add(port);
    } while (confirm("Add another port?"));
    return ports;
  }

  private List<String> collectCommands() {
    var commands = new ArrayList<String>();
    do {
      var command = question("Command", false);
      if (command == null || command.isBlank()) break;
      commands.add(command);
    } while (confirm("Add another command?"));
    return commands;
  }

  private List<String> collectDependencies(Set<String> existingServices) {
    var dependsOn = new ArrayList<String>();
    do {
      var dependency = existingServices.isEmpty()
          ? question("Depends on?")
          : options("Depends on?", existingServices.stream().toList());
      if (dependency == null || dependency.isBlank()) break;
      dependsOn.add(dependency);
    } while (confirm("Add another dependency?"));
    return dependsOn;
  }

  private Map<String, String> collectEnvironmentVariables() {
    var environments = new LinkedHashMap<String, String>();
    do {
      var envVariable = question("Environment variable", false);
      if (envVariable == null || !envVariable.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) break;
      environments.put(envVariable, question("Environment variable value", false));
    } while (confirm("Add another environment variable?"));
    return environments;
  }

  private List<String> collectVolumes() {
    var volumes = new ArrayList<String>();
    do {
      var volume = question("Volume", false);
      if (volume == null || volume.isBlank()) break;
      volumes.add(volume);
    } while (confirm("Add another volume?"));
    return volumes;
  }

  private List<String> collectNetworks() {
    var networks = new ArrayList<String>();
    do {
      var network = question("Network", false);
      if (network == null || network.isBlank()) break;
      networks.add(network);
    } while (confirm("Add another network?"));
    return networks;
  }

  private Map<String, String> collectLabels() {
    var labels = new LinkedHashMap<String, String>();
    do {
      var label = question("Label", false);
      if (label == null || label.isBlank()) break;
      labels.put(label, question("Label value", false));
    } while (confirm("Add another label?"));
    return labels;
  }

  private HealthCheck createHealthcheck() {
    if (!confirm("Do you want to add healthcheck?")) {
      return null;
    }
    return new HealthCheck(
        List.of(question("Healthcheck command", false)),
        question("Healthcheck interval", false),
        question("Healthcheck timeout", false),
        ofNullable(question("Healthcheck retries", false)).map(Integer::parseInt).orElse(0),
        question("Healthcheck start period", false)
    );
  }

  private Map<String, Network> createNetworks() {
    var networks = new LinkedHashMap<String, Network>();
    do {
      var networkName = question("Network name", true);
      var driver = options("Network driver", List.of(NetworkDriver.values()), s -> s.name().toLowerCase());
      var external = confirm("Is this network external?");
      var driverOpts = collectDriverOptions("Network");
      var labels = collectLabels();

      networks.put(networkName, new Network(
          ofNullable(driver).orElse(BRIDGE).name().toLowerCase(),
          external,
          driverOpts,
          labels
      ));
    } while (confirm("Add another network?"));
    return networks;
  }

  private Map<String, Volume> createVolumes() {
    var volumes = new LinkedHashMap<String, Volume>();
    do {
      var volumeName = question("Volume name", true);
      var external = confirm("Is this volume external?");
      var driver = options("Volume driver", List.of(VolumeDriver.values()), s -> s.name().toLowerCase());
      var driverOpts = collectDriverOptions("Volume");
      var labels = collectLabels();

      volumes.put(volumeName, new Volume(
          ofNullable(driver).orElse(VolumeDriver.LOCAL).name().toLowerCase(),
          external,
          driverOpts,
          labels
      ));
    } while (confirm("Add another volume?"));
    return volumes;
  }

  private Map<String, String> collectDriverOptions(String type) {
    var driverOpts = new LinkedHashMap<String, String>();
    do {
      var driverOpt = question(type + " driver option", false);
      if (driverOpt == null || driverOpt.isBlank()) break;
      driverOpts.put(driverOpt, question(type + " driver option value", false));
    } while (confirm("Add another " + type.toLowerCase() + " driver option?"));
    return driverOpts;
  }

  private void writeToFile(String fileName, String content) {
    try {
      var path = new File(output, fileName).toPath();
      if (!Files.exists(path)) {
        if (path.getParent() != null && !Files.exists(path.getParent())) {
          logger.info("Creating file: {}", path);
          Files.createDirectories(path.getParent());
        }
        Files.createFile(path);
      }

      Files.writeString(new File(output, fileName).toPath(), content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void generateDockerfile() {
    var dockerfileName = question("Dockerfile name", false);
    if (!checkFileOverwrite(dockerfileName)) {
      return;
    }

    var stages = createDockerfileStages();
    var dockerfile = new Dockerfile(dockerfileName, stages);
    writeToFile(dockerfileName, dockerfile.toString());
  }

  private List<DockerStage> createDockerfileStages() {
    var stages = new ArrayList<DockerStage>();
    do {
      var stage = createDockerStage();
      stages.add(stage);
    } while (confirm("Add another stage?"));
    return stages;
  }

  private DockerStage createDockerStage() {
    var stageName = question("Stage name", false);
    var imageName = question("Image name", true);
    var images = searchImagesWithTags(imageName);
    var image = options("Choose an image", images, DockerImage::name);
    var from = new From(image.toString(), stageName);
    var resources = createDockerResources();
    return new DockerStage(stageName, from, resources);
  }

  private List<DockerfileResource> createDockerResources() {
    var resources = new ArrayList<DockerfileResource>();
    do {
      var resourceType = options("Choose a Docker resource type",
          List.of(DockerfileResourceType.values()),
          s -> s.name().toLowerCase());
      var resource = createDockerResource(resourceType);
      if (resource == null) {
        break;
      }
      resources.add(resource);
    } while (true);
    return resources;
  }

  private DockerfileResource createDockerResource(DockerfileResourceType resourceType) {
    return switch (resourceType) {
      case COPY -> createCopyResource();
      case RUN -> createRunResource();
      case EXPOSE -> createExposeResource();
      case ENV -> createEnvResource();
      case WORKDIR -> createWorkdirResource();
      case ADD -> createAddResource();
      case ENTRYPOINT -> createEntrypointResource();
      case CMD -> createCmdResource();
      default -> null;
    };
  }

  private DockerfileResource createCmdResource() {
    return new Cmd(question("Command", true));
  }

  private Copy createCopyResource() {
    var copyFrom = question("Copy from", true);
    var copyTo = question("Copy to", true);
    if ((copyFrom == null || copyFrom.isBlank()) || (copyTo == null || copyTo.isBlank())) {
      return null;
    }
    return new Copy(copyFrom, copyTo);
  }

  private Run createRunResource() {
    var runCommand = question("Run command", true);
    if (runCommand == null || runCommand.isBlank()) {
      return null;
    }
    return new Run(runCommand);
  }

  private Expose createExposeResource() {
    var exposePort = question("Expose port", true);
    if (exposePort == null || !exposePort.matches("\\d{2,6}")) {
      return null;
    }
    return new Expose(Integer.parseInt(exposePort));
  }

  private Env createEnvResource() {
    var envVariable = question("Environment variable", true);
    if (envVariable == null || !envVariable.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
      return null;
    }
    return new Env(envVariable);
  }

  private Workdir createWorkdirResource() {
    var workDir = question("Work directory", true);
    if (workDir == null || workDir.isBlank()) {
      return null;
    }
    return new Workdir(workDir);
  }

  private Add createAddResource() {
    var addFrom = question("Add from", true);
    var addTo = question("Add to", true);
    if ((addFrom == null || addFrom.isBlank()) || (addTo == null || addTo.isBlank())) {
      return null;
    }
    return new Add(addFrom, addTo);
  }

  private Entrypoint createEntrypointResource() {
    var entrypoint = question("Entrypoint", true);
    if (entrypoint == null || entrypoint.isBlank()) {
      return null;
    }
    return new Entrypoint(entrypoint);
  }
}
