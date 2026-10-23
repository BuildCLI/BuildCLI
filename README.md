```
,-----.          ,--.,--.   ,--. ,-----.,--.   ,--.
|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |
|  .-.  \|  ||  |,--.|  |' .-. ||  |    |  |   |  |       Built by the community, for the community
|  '--' /'  ''  '|  ||  | \`-' |'  '--'\|  '--.|  |
\`------'  \`----' \`--'\`--' \`---'  \`-----'\`-----'\`--'

Welcome to BuildCLI - Java Project Management!
```

# BuildCLI

**BuildCLI** is a command-line interface (CLI) for managing and automating common tasks in Java project development. It helps developers initialize projects, compile source code, manage dependencies, configure application profiles, run applications, generate documentation, and automate development workflows directly from the terminal.

The project also provides Docker integration, CI/CD configuration generation, changelog generation, and Docker Compose configuration support.

- **Repository:** [BuildCLI on GitHub](https://github.com/BuildCLI/BuildCLI)
- **License:** [MIT License](https://opensource.org/licenses/MIT)
- **Documentation:** See the repository's `PROJECT_FAMILIARIZATION.md` file for additional project information.

---

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [Usage and Examples](#usage-and-examples)
- [AI-Powered Documentation](#ai-powered-documentation)
- [Configuration Profiles](#configuration-profiles)
- [Docker Integration](#docker-integration)
- [CI/CD Integration](#cicd-integration)
- [Changelog Generation](#changelog-generation)
- [Docker Compose](#docker-compose)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Features

BuildCLI provides command-line tools for common Java development tasks.

- **Project Initialization:** Creates the basic directory structure and files for a Java project.
- **Project Compilation:** Compiles Java source code using Maven.
- **Dependency Management:** Adds and removes dependencies in `pom.xml`.
- **AI-Powered Documentation (Beta):** Generates documentation and comments for Java source files using a locally accessible Ollama API.
- **Configuration Profiles:** Creates profile-specific configuration files, such as `application-dev.properties` and `application-test.properties`.
- **Project Execution:** Runs supported Java projects using Spring Boot.
- **Dockerfile Generation:** Generates a Dockerfile for containerizing Java applications.
- **Docker Build and Run:** Builds and runs Docker containers for supported projects.
- **CI/CD Configuration:** Generates pipeline configuration files for GitHub Actions, GitLab, and Jenkins.
- **Changelog Generation:** Creates structured changelogs from Git commit history in Markdown, HTML, or JSON.
- **Docker Compose Configuration:** Generates a Docker Compose configuration for a Java application.

---

## Prerequisites

Install the tools required for the features you intend to use.

| Tool | Purpose |
|---|---|
| Java | Running Java applications and Java-based tooling |
| Git | Version control and changelog generation |
| Apache Maven | Building Java projects and resolving dependencies |
| Docker | Building and running containers |
| Ollama | Required for AI-powered code documentation |
| Jenkins, GitLab, or GitHub Actions | Relevant CI/CD integrations |

**Note:** The required Java version and other version-specific requirements should be checked against the project's build configuration and release documentation.

---

## Installation

BuildCLI provides installation scripts and downloadable release artifacts.

### Option 1: Installation Script

Download or obtain the installation scripts, `install.sh` and `install.bat`, from the project repository.

**Linux and macOS**

```bash
chmod +x install.sh
./install.sh
```

**Windows**

Run `install.bat` from Command Prompt or by double-clicking the file.

The installation-script method requires Git and Maven to be installed.

### Option 2: Install from a Release

Download the appropriate BuildCLI release from the project's GitHub repository:

[BuildCLI Releases](https://github.com/BuildCLI/BuildCLI/releases)

#### Windows

1. Download the release containing `buildcli.jar` and `buildcli.bat`.
2. Place both files in the same directory.
3. Add that directory to the system `PATH`, if necessary.
4. Open a new terminal and run:

```bat
buildcli
```

#### Linux

1. Download the release containing `buildcli.jar` and the `buildcli` launcher.
2. Place both files in the same directory, such as `/usr/local/bin`.
3. Ensure the launcher has execution permission.
4. Run BuildCLI from the terminal.

```bash
chmod +x /usr/local/bin/buildcli
buildcli
```

Ensure the JAR file is available at the location expected by the launcher.

#### macOS

1. Download the appropriate BuildCLI release.
2. Place the JAR file and launcher in the same directory.
3. Ensure the launcher has execution permission.
4. Make the launcher directory available through your `PATH`.

```bash
chmod +x /usr/local/bin/buildcli
buildcli
```

Use a directory you control if you do not have permission to write to `/usr/local/bin`.

---

## Getting Started

BuildCLI has undergone architectural changes. Start by checking the commands supported by your installed version:

```bash
buildcli help
```

You can also consult the repository's issue and pull request history for additional context about the command-line interface.

### Initialize a Project

Create a new Java project:

```bash
buildcli project init MyProject
```

To initialize a project without providing a name:

```bash
buildcli project init
```

The initialization command creates the basic project structure, including source directories, a Maven `pom.xml`, and a README file.

### Compile a Project

Compile the project using Maven:

```bash
buildcli project build --compile
```

### Add a Dependency

Add a dependency using its Maven coordinates:

```bash
buildcli project add dependency org.springframework:spring-core
```

To specify a version explicitly:

```bash
buildcli p a d org.springframework:spring-core:5.3.21
```

The dependency is added to the project's `pom.xml`.

For reproducible builds, specify the dependency version explicitly when appropriate.

### Remove a Dependency

BuildCLI supports removing dependencies from `pom.xml`.

Consult the command help for the installed version to identify the exact syntax:

```bash
buildcli help
```

### Run a Project

Run a supported Spring Boot project:

```bash
buildcli run
```

### Create a Configuration Profile

Create a development profile:

```bash
buildcli project add profile dev
```

This creates a profile-specific configuration file, such as `application-dev.properties`.

---

## Usage and Examples

The following examples demonstrate documented BuildCLI commands. Commands and available options can vary between versions, so consult `buildcli help` when working with a different release.

### Example 1: Initialize a Project

```bash
buildcli project init MyProject
```

### Example 2: Compile the Project

```bash
buildcli project build --compile
```

### Example 3: Add a Maven Dependency

```bash
buildcli project add dependency org.springframework:spring-core
```

### Example 4: Create a Development Profile

```bash
buildcli project add profile dev
```

### Example 5: Run the Application

```bash
buildcli run
```

### Example 6: Generate a Dockerfile

```bash
buildcli p add dockerfile
```

### Example 7: Generate a CI/CD Pipeline

For GitHub Actions:

```bash
buildcli project add pipeline github
```

For GitLab:

```bash
buildcli project add pipeline gitlab
```

For Jenkins:

```bash
buildcli project add pipeline jenkins
```

---

## AI-Powered Documentation

BuildCLI includes a beta feature that uses AI to generate documentation and comments for Java source code.

This feature relies on an Ollama server accessible through a local API.

### Prerequisites

1. Install Ollama using the official instructions: [Ollama](https://ollama.com/).
2. Ensure Ollama is running.
3. Make the required model available locally.

For example, the following command runs the `llama3.2` model:

```bash
ollama run llama3.2
```

### Generate Code Documentation

For a Java source file:

```bash
buildcli ai code document File.java
```

The command sends the specified source file to the locally accessible Ollama service to generate documentation and comments. The documented file is saved back to its original location.

**Important:** Review AI-generated documentation before committing changes. Confirm that the comments accurately describe the implementation and do not introduce incorrect assumptions.

---

## Configuration Profiles

BuildCLI supports the creation of profile-specific application configuration files.

For example, create a development profile:

```bash
buildcli project add profile dev
```

A profile can correspond to a file such as:

```text
application-dev.properties
```

BuildCLI also provides functionality for selecting an active environment profile.

### Set the Active Environment

Set the development environment:

```bash
buildcli p set env dev
```

The selected profile is saved in `environment.config` and is used by the application's execution workflow.

When using the documented environment-setting functionality, the active profile can be displayed during execution.

**Security recommendation:** Keep credentials, API keys, and other secrets out of committed configuration files. Use appropriate environment-specific secret management practices.

---

## Docker Integration

BuildCLI includes commands for generating a Dockerfile and building and running a Docker container.

### Generate a Dockerfile

```bash
buildcli p add dockerfile
```

This command generates a Dockerfile for the Java project.

### Build and Run a Container

```bash
buildcli project run docker
```

The command builds a Docker image and runs the application in a container.

### Requirements

- Docker must be installed and accessible.
- The Docker daemon must be running.
- The Java project must contain the files and configuration required for the generated container build.

Review the generated Dockerfile before deploying an application to production.

---

## CI/CD Integration

BuildCLI can generate configuration files for supported continuous integration and continuous delivery platforms.

### GitHub Actions

```bash
buildcli project add pipeline github
```

### GitLab

```bash
buildcli project add pipeline gitlab
```

### Jenkins

```bash
buildcli project add pipeline jenkins
```

These commands generate pipeline configuration files for the selected platform.

**Note:** Generating a pipeline configuration does not necessarily configure credentials, repository permissions, deployment environments, or external services. Those settings may require additional configuration in the relevant platform.

---

## Changelog Generation

BuildCLI provides a changelog generator that analyzes Git commit history and creates a structured changelog.

Supported output formats include:

- Markdown
- HTML
- JSON

The command is available through `changelog` and the `cl` alias.

### Basic Usage

```bash
buildcli changelog
```

Alternatively:

```bash
buildcli cl
```

### Specify a Version and Output Format

Generate a Markdown changelog for version `v1.0.0`:

```bash
buildcli changelog --version v1.0.0 --format markdown --output CHANGELOG.md
```

The short-option form is also supported:

```bash
buildcli changelog -v v1.0.0 -f markdown -o CHANGELOG.md
```

### Available Options

| Option | Short form | Description |
|---|---|---|
| `--version` | `-v` | Specifies the release version. |
| `--format` | `-f` | Selects the output format: `markdown`, `html`, or `json`. |
| `--output` | `-o` | Specifies the output filename. |
| `--include` | `-i` | Selects commit types to include, separated by commas. |

When a version is not provided, BuildCLI attempts to detect the latest Git tag. If no tag is found, it uses `Unreleased`.

### Filter Commit Types

Generate a changelog containing feature and bug-fix commits:

```bash
buildcli changelog --version v1.0.0 --format markdown --include feat,fix --output CHANGELOG.md
```

The `--include` option accepts a comma-separated list of commit types, such as:

- `feat`
- `fix`
- `docs`
- `refactor`

### Generate HTML or JSON

HTML output:

```bash
buildcli changelog --version v1.0.0 --format html --output CHANGELOG.html
```

JSON output:

```bash
buildcli changelog --version v1.0.0 --format json --output CHANGELOG.json
```

For accurate results, run changelog generation in a Git repository containing the relevant commit history and tags.

---

## Docker Compose

BuildCLI includes functionality for generating a Docker Compose configuration for a Java application.

### Generate a Compose File

```bash
buildcli project add dockerCompose
```

The documented alias is:

```bash
buildcli p add dc
```

The command generates a `docker-compose.yml` file containing a primary service for the Java application and using the image built from the generated Dockerfile.

Review the generated Compose configuration before starting containers or deploying the application.

---

## Troubleshooting

### The `buildcli` Command Is Not Recognized

- Confirm that the launcher exists.
- Verify that its directory is included in your system `PATH`.
- Ensure that the launcher can locate `buildcli.jar`.
- Open a new terminal after changing environment variables.

### Maven Build Errors

- Verify that Java and Maven are installed.
- Confirm that the project contains a valid `pom.xml`.
- Check the Maven output for dependency-resolution or compilation errors.

### Docker Commands Fail

- Confirm that Docker is installed and the daemon is running.
- Check that the current user has permission to access Docker.
- Review the generated Dockerfile and build output.

### AI Documentation Does Not Work

- Confirm that Ollama is installed and running.
- Verify that the requested model is available locally.
- Check that BuildCLI can reach the Ollama API.
- Review the command help for any version-specific requirements.

### A Command Is Not Available

BuildCLI's command-line architecture has changed over time. Check the installed version's help output:

```bash
buildcli help
```

If the problem persists, report it through the project's issue tracker.

---

## Contributing

Contributions are welcome! You can help improve BuildCLI by reporting bugs, suggesting features, improving documentation, or submitting code changes.

### Contribution Workflow

1. Fork the repository.
2. Clone your fork.
3. Create a branch for your changes.
4. Implement and test your changes.
5. Commit your changes.
6. Push the branch to your fork.
7. Open a pull request against the main repository.

### Example Workflow

Create a feature branch:

```bash
git checkout -b feature/my-feature
```

Commit your changes:

```bash
git add .
git commit -m "Add my feature"
```

Push your branch:

```bash
git push origin feature/my-feature
```

Then open a pull request on GitHub and describe the changes.

### Before Submitting a Pull Request

- Keep changes focused and easy to review.
- Follow the conventions already used in the project.
- Test the affected functionality.
- Update documentation when commands or behavior change.
- Avoid committing credentials, secrets, generated artifacts, or unrelated changes.

For additional project-specific guidance, consult `CONTRIBUTING.md` if it is present in the repository.

---

## License

BuildCLI is distributed under the MIT License.

See the repository's `LICENSE` file for the complete license text.

---

## Project Information

- **Project:** BuildCLI
- **Purpose:** Java project management and development automation from the command line
- **Repository:** https://github.com/BuildCLI/BuildCLI
- **License:** MIT

For further information about the project's structure, commands, and implementation, consult the repository documentation and source code.

**Built by the community, for the community.**
