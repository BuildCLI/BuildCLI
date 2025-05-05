```
,-----.          ,--.,--.   ,--. ,-----.,--.   ,--.
|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |
|  .-.  \|  ||  |,--.|  |' .-. ||  |    |  |   |  |       Built by the community, for the community
|  '--' /'  ''  '|  ||  |\ `-' |'  '--'\|  '--.|  |
`------'  `----' `--'`--' `---'  `-----'`-----'`--'

Welcome to BuildCLI - Java Project Management!
```

# BuildCLI

**BuildCLI** is a command-line interface (CLI) tool for managing and automating common tasks in Java
project development. It allows you to create, compile, manage dependencies, and run Java projects
directly from the terminal, simplifying the development process.

- **Repository:** [https://github.com/BuildCLI/BuildCLI](https://github.com/BuildCLI/BuildCLI)
- **License:** [MIT](https://opensource.org/licenses/MIT)

---

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Examples](#examples)
- [Contribution](#contribution)
- [License](#license)

---

## Features

- **Initialize Project**: Creates the basic structure of directories and files for a Java project.
- **Compile Project**: Compiles the project source code using Maven.
- **Add Dependency**: Adds new dependencies to the `pom.xml`.
- **Remove Dependency**: Remove dependencies from `pom.xml`.
- **Document Code**: [Beta] Generates documentation for a Java file using AI.
- **Manage Configuration Profiles**: Creates specific configuration files for profiles (
  `application-dev.properties`, `application-test.properties`, etc.).
- **Run Project**: Starts the project directly from the CLI using Spring Boot.
- **Dockerize Project**: Generates a Dockerfile for the project, allowing easy containerization.
- **Build and Run Docker Container**: Builds and runs the Docker container using the generated
  Dockerfile.
- **CI/CD Integration**: Automatically generates configuration files por CI/CD tools (e.g., Jenkins,
  GitHub Actions) and triggers pipelines based on project changes.
- **Changelog Generation**: Automatically generates a structured changelog by analyzing the Git
  commit history, facilitating the understanding of changes between releases.
- **Orchestration Commands**: BuildCLI now includes orchestration commands to automate Docker
  Compose configuration generation and manage container lifecycles.

---

## Installation

<details>
<summary>Installing from script</summary>

> [!TIP]
> This installation method requires git and maven to be installed on your system.

1. **Script Installation**:
   Download the [install.sh](install.sh) or [install.bat](install.bat) file and execute.

    - On a Unix-like system (Linux, macOS), simply give execution permission to `install.sh` and run
      it:

     ```bash
     sudo chmod +x install.sh  
     ./install.sh  
     ```

    - On Windows: Run `install.bat` by double-clicking it or executing the following command in the
      Command Prompt (cmd):

     ```cmd
     install.bat
     ```

Now `BuildCLI` is ready to use. Test the `buildcli` command in the terminal.
</details>

<details>
<summary>Installing from Releases</summary>

> [!TIP]
> You can build the project with maven and run the wrapper script to start the CLI so that you can
> use the CLI without `java -jar buildcli.jar` command.

### Windows

1. Download the [latest version](https://github.com/brenoepics/BuildCLI/releases/latest) of
   BuildCLI.
2. Move both `buildcli.jar` and `buildcli.bat` to your Windows directory. (Usually `C://Windows`)
3. If you do not have access to `C://Windows`, you can place the two files anywhere and add that
   directory to your Environment Variables System PATH variable.
4. Try running `buildcli` via the command prompt.

### Linux

1. Download the [latest version](https://github.com/brenoepics/BuildCLI/releases/latest) of
   BuildCLI.
2. Move both `buildcli.jar` and `buildcli` to `/usr/local/bin`. (root needed)
3. Make sure both files are executable. (`chmod +x`)
4. Try running `buildcli` via CLI.

### Mac

1. Download the [latest version](https://github.com/brenoepics/BuildCLI/releases/latest) of
   BuildCLI.
2. Move both `buildcli.jar` and `buildcli` to `/usr/local/bin`. (root needed)
3. Make sure both files are executable. (`chmod +x`)
4. Try running `buildcli` via CLI.

</details>

---

## Usage

We made a major refactor of the `BuildCLI` architecture. Please use the `buildcli help` command to
see all available options. Also, refer to
issue [#89](https://github.com/wheslleyrimar/BuildCLI/issues/89) and pull
request [#79](https://github.com/wheslleyrimar/BuildCLI/pull/79) for more details.

---

## Examples

### 1. Initialize a New Project

Creates the basic Java project structure, including `src/main/java`, `pom.xml`, and `README.md`.
You can specify a project name to dynamically set the package structure and project artifact.

#### Example Commands

- To initialize a project with a specific name:

```bash
buildcli project init MyProject
```

This will create the project structure with `MyProject` as the base package name, resulting in a
directory like `src/main/java/org/myproject`.

- To initialize a project without specifying a name:

```bash
buildcli project init
```

This will create the project structure with `buildcli` as the base package name, resulting in a
directory like `src/main/java/org/buildcli`.

### 2. Compile the Project

Compiles the Java project using Maven:

```bash
buildcli project build --compile
```

### 3. Add a Dependency to `pom.xml`

Adds a dependency to the project in the `groupId:artifactId` format. You can also specify a version
using the format `groupId:artifactId:version`. If no version is specified, the dependency will
default to the latest version available.

#### Example Commands

- To add a dependency with the latest version:

```bash
  buildcli project add dependency org.springframework:spring-core
```

- To add a dependency with a specified version:

```bash
  buildcli p a d org.springframework:spring-core:5.3.21
```

After executing these commands, the dependency will be appended to your pom.xml file under the
`<dependencies>` section.

### 4. Create a Configuration Profile

Creates a configuration file with the specified profile, for example, `application-dev.properties`:

```bash
buildcli project add profile dev
```

### 5. Run the Project

Runs the Java project using Spring Boot:

```bash
buildcli project run
```

### 6. Generate Documentation for Java Code

Automatically generates inline documentation for a Java file using AI:

```bash
# File or directory
buildcli ai code document File.java 
```

This command sends the specified Java file to the local Ollama server, which generates documentation
and comments directly within the code. The modified file with documentation will be saved back to
the same location.

### 7. Set Active Environment Profile

Sets the active environment profile, saving it to the `environment.config` file. The profile is
referenced during project execution, ensuring that the correct configuration is loaded.

```bash
buildcli p set env dev
```

After running this command, the active profile is set to dev, and the `environment.config` file is
updated accordingly.

#### Active Profile Display During Project Execution

With the `--set-environment` functionality, you can set the active environment profile. When running
the project with `buildcli --run`, the active profile will be displayed in the terminal.

### 8. Dockerize Command

This command generates a `Dockerfile` for your Java project, making it easier to containerize your
application.

```bash
buildcli p add dockerfile
```

### 9. Docker Build Command

This command automatically builds and runs the Docker container for you. After running the command,
the Docker image will be created, and your project will run inside the container.

```bash
buildcli project run docker
```

### 10. Set Up CI/CD Integration

Generates configuration files for CI/CD tools and prepares the project for automated pipelines.
Supports Jenkins, Gitlab and GitHub Actions.

```bash
buildcli project add pipeline github
```

```bash
buildcli project add pipeline gitlab
```

```bash
buildcli project add pipeline jenkins
```

### 11. Changelog Generation

BuildCLI now includes an automatic changelog generation feature that analyzes your Git commit
history and produces a structured changelog.
This helps developers and end-users easily track changes between releases.

### Usage Instructions

To generate a changelog, run:

   ```bash
   buildcli changelog [OPTIONS]
   ```

Or use the alias:

```bash
   buildcli cl [OPTIONS]
   ```

### Options:

- `--version, -v <version>:`
  Specify the release version for the changelog. If omitted, BuildCLI attempts to detect the latest
  Git tag. If no tag is found, it defaults to "Unreleased".

- `--format, -f <format>:`
  Specify the output format. Supported formats:

    - markdown (default)
    - html
    - json
- `--output, -o <file>:`
  Specify the output file name. If not provided, defaults to CHANGELOG.<extension>.

- `--include, -i <commit types>:`
  Provide a comma-separated list of commit types to include (e.g., feat,fix,docs,refactor).

### Example Command

```bash
buildcli changelog --version v1.0.0 --format markdown --include feat,fix --output CHANGELOG.md
````

#### or

```bash
buildcli changelog -v v1.0.0 -f markdown -i feat,fix -o CHANGELOG.md
````

### 12. Orchestration Commands

BuildCLI now includes orchestration commands to automate Docker compose configuration generation and
manage
container lifecycles.
Below, you'll find a Quick Start Guide and usage examples for the new feature.

---

### Quick Start Guide

### 1. Generate a Docker Compose file for your project:

```bash
buildcli project add dockerCompose
```

or

```bash
buildcli p add dc
```

This command creates a docker-compose.yml file with a primary service for your java application,
using the
image built from the enhanced Dockerfile.

### 2. Customize Essential Parameters:

You can customize ports, volumes, and resource limits (CPU and memory) using CLI flag. For example:

#### Options:

- `--port, -p <ports>:`
  Specify the ports to expose for the container. Format: `<host_port>:<container_port>`.

- `--volume, -v <volume>:`
  Specify the volume to mount for the container. Format: `<host_path>:<container_path>`.

- `--cpu, -c <cpu_limit>:`
  Specify the CPU limit for the container.

- `--memory, -m <memory_limit>:`
  Specify the memory limit for the container. Format: `<value><unit>` (e.g., `512m`).

- `--dockerfile, -d <dockerfile_path>:`
- Specify the path to the Dockerfile for the container.

#### Example Command

```bash
buildcli project add docker-compose --ports 8080:8080 --volumes /data:/app/data --cpu 2 --memory 512m --dockerfile /path/to/Dockerfile
```

or

```bash
buildcli p add dc -p 8080:8080 -v /data:/app/data -c 2 -m 512m -d /path/to/Dockerfile
```

### 3. Start the Containers:

Use the following command to start the containers:

```bash
buildcli run orchrestration up
```

or

```bash
buildcli run oc up
```

To force a rebuild of the images, add the `--build` flag:

```bash
buildcli run orchestration up --build
```

or

```bash
buildcli run oc up -b
```

### 4. Stop the Containers:

To stop the containers, use the following command:

```bash
buildcli run orchestration down
``` 

or

```bash
buildcli run oc down
```

### 5. Stop a specific container:

To stop a specific container, use the following command:

```bash
buildcli orchestration down --name <container_name>
``` 

or

```bash
buildcli oc down -n <container_name>
```

---

## Prerequisites

### Local Ollama API

Ensure you have the Ollama server running locally, as the `docs` functionality relies on an AI model
accessible via a local API.

- [Download Ollama](https://ollama.com/download)

You can start the Ollama server by running:

```bash
ollama run llama3.2
```

### Prerequisites for CI/CD Integration

- **Jenkins**: Ensure Jenkins is installed and accessible in your environment.
- **GitHub Actions**: Ensure your repository is hosted on GitHub with Actions enabled.

---

## Contribution

Contributions are welcome! Feel free to open **Issues** and submit **Pull Requests**.
See the [CONTRIBUTING.md](/.github/CONTRIBUTING.md) file for more details.

Quick steps to contribute:

1. Fork the project.
2. Create a branch for your changes:

   ```bash
   git checkout -b feature/my-feature
   ```

3. Commit your changes:

   ```bash
   git commit -m "My new feature"
   ```

4. Push to your branch:

   ```bash
   git push origin feature/my-feature
   ```

5. Open a Pull Request in the main repository.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Familiarizing Yourself with BuildCLI

To get a deeper understanding of the BuildCLI project structure, key classes, commands, and how to
contribute, check out our comprehensive guide
in [PROJECT_FAMILIARIZATION.md](PROJECT_FAMILIARIZATION.md).
