BuildCLI

<p align="center">
  <img src="https://img.shields.io/badge/Java-Project%20Management-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java Project Management" />
  <img src="https://img.shields.io/badge/CLI-Developer%20Tools-blue?style=for-the-badge&logo=windows-terminal&logoColor=white" alt="Command Line Interface" />
</p><p align="center">
  <strong>A powerful command-line toolkit for Java project development and automation.</strong>
</p><p align="center">
  Simplify project initialization, builds, dependency management, documentation, containerization, and development workflows — directly from your terminal.
</p><p align="center">
  <a href="https://github.com/BuildCLI/BuildCLI">Repository</a> •
  <a href="https://github.com/BuildCLI/BuildCLI/issues">Issues</a> •
  <a href="https://github.com/BuildCLI/BuildCLI/releases">Releases</a> •
  <a href="https://github.com/BuildCLI/BuildCLI/blob/master/PROJECT_FAMILIARIZATION.md">Project Guide</a>
</p>---

Overview

BuildCLI is an open-source command-line interface designed to simplify common tasks in Java project development.

By bringing frequently used development operations into a unified CLI, BuildCLI helps developers initialize projects, compile source code, manage Maven dependencies, configure application profiles, run applications, and automate delivery workflows.

Beyond core project management, BuildCLI provides integrations for Docker, Docker Compose, CI/CD pipeline configuration, AI-assisted code documentation, and automated changelog generation.

Whether you're starting a new Java application or streamlining an existing workflow, BuildCLI aims to make development tasks more accessible and repeatable.

Table of Contents

- "Features" (#features)
- "Technology Stack" (#technology-stack)
- "Prerequisites" (#prerequisites)
- "Installation" (#installation)
- "Quick Start" (#quick-start)
- "Command Reference" (#command-reference)
- "AI-Powered Documentation" (#ai-powered-documentation)
- "Configuration Profiles" (#configuration-profiles)
- "Docker Integration" (#docker-integration)
- "Docker Compose" (#docker-compose)
- "CI/CD Integration" (#cicd-integration)
- "Changelog Generation" (#changelog-generation)
- "Global Git Ignore Configuration" (#global-git-ignore-configuration)
- "Security Best Practices" (#security-best-practices)
- "Troubleshooting" (#troubleshooting)
- "Contributing" (#contributing)
- "License" (#license)

Features

Feature| Description
Project Initialization| Generate a basic Java project structure and Maven configuration.
Project Compilation| Compile Java projects using Maven.
Dependency Management| Add and remove dependencies in "pom.xml".
AI-Assisted Documentation| Generate Java documentation and comments using a locally accessible Ollama service.
Configuration Profiles| Create environment-specific application configuration files.
Application Execution| Run supported Java and Spring Boot projects.
Docker Integration| Generate Dockerfiles and build and run application containers.
Docker Compose| Generate Compose configurations and support container orchestration workflows.
CI/CD Generation| Generate pipeline configuration files for GitHub Actions, GitLab, and Jenkins.
Changelog Generation| Create Markdown, HTML, or JSON changelogs from Git history.

«Note: AI-powered documentation and other integrations may have version-specific requirements. Check the command help and repository documentation for your installed version.»

Technology Stack

BuildCLI works with tools commonly used in Java development:

- Java — application development and execution.
- Apache Maven — project builds and dependency resolution.
- Git — source control and changelog generation.
- Docker — container image creation and execution.
- Docker Compose — multi-container application workflows.
- Ollama — local AI-assisted code documentation.
- GitHub Actions, GitLab CI/CD, and Jenkins — CI/CD configuration generation.

The precise versions required depend on the BuildCLI release and the Java project being managed.

Prerequisites

Install the tools needed for the features you plan to use.

Requirement| When it's needed
Java| Running Java applications and Java-based tooling.
Git| Version control and changelog generation.
Apache Maven| Building Java projects and resolving dependencies.
Docker| Container image builds and execution.
Docker Compose| Compose-based orchestration workflows.
Ollama| AI-assisted documentation.
A supported CI/CD platform| Using generated pipeline configurations.

Verify your local installation:

java -version
git --version
mvn -version
docker --version
docker compose version

Docker and Ollama checks are only necessary if you intend to use those features.

Installation

BuildCLI provides installation scripts and downloadable release artifacts.

Option 1: Installation Scripts

Obtain "install.sh" or "install.bat" from the "BuildCLI repository" (https://github.com/BuildCLI/BuildCLI).

Linux and macOS

chmod +x install.sh
./install.sh

Windows

Run "install.bat" from Command Prompt:

install.bat

The script-based installation method requires Git and Maven, as indicated by the project's installation documentation.

Option 2: GitHub Releases

Download the appropriate files from the "BuildCLI Releases page" (https://github.com/BuildCLI/BuildCLI/releases).

Windows

1. Download "buildcli.jar" and "buildcli.bat".
2. Place both files in the same directory.
3. Add that directory to your system "PATH" if needed.
4. Open a new terminal and run:

buildcli

Linux

1. Download "buildcli.jar" and the "buildcli" launcher.
2. Place them in a suitable directory.
3. Ensure the launcher is executable.
4. Confirm that the launcher can locate the JAR file.

For example, if the launcher is installed under "/usr/local/bin":

sudo install -d /usr/local/bin
sudo install -m 755 buildcli /usr/local/bin/buildcli
sudo install -m 644 buildcli.jar /usr/local/bin/buildcli.jar

Important: The launcher must expect the JAR at the location where you install it. Adjust the installation paths if the launcher uses a different layout.

macOS

1. Download the appropriate release artifacts.
2. Place the launcher and JAR in a suitable directory.
3. Make the launcher executable.
4. Add its directory to your "PATH", if necessary.

chmod +x buildcli
./buildcli

If you use a system-wide installation directory, make sure you have the required permissions and that the launcher can locate the JAR.

Verify the Installation

Run:

buildcli help

Use the displayed help information as the reference for commands and options supported by your installed version.

Quick Start

Get started with a new Java project.

1. Initialize a Project

buildcli project init MyProject

To initialize a project without specifying a name:

buildcli project init

The initialization workflow creates a basic project structure, including Java source directories, a Maven "pom.xml", and a README file.

2. Compile the Project

buildcli project build --compile

This command invokes the project's compilation workflow.

3. Add a Dependency

buildcli project add dependency org.springframework:spring-core

To specify a version explicitly:

buildcli p a d org.springframework:spring-core:5.3.21

Review the resulting "pom.xml" to verify the dependency coordinates and version.

4. Run the Application

For a supported Spring Boot project:

buildcli run

5. Explore Available Commands

buildcli help

Tip: Start with the help output whenever a command behaves differently from the examples in this README.

Command Reference

The following examples reflect the command syntax provided in the project's documentation. Options and aliases may differ between releases.

Project Management

Operation| Command
Initialize a named project| "buildcli project init MyProject"
Initialize without a name| "buildcli project init"
Compile a project| "buildcli project build --compile"
Add a Maven dependency| "buildcli project add dependency org.springframework:spring-core"
Remove a dependency| Consult "buildcli help" for the installed version.
Create a development profile| "buildcli project add profile dev"
Run a supported application| "buildcli run"

Generate a Dockerfile

buildcli p add dockerfile

Generate CI/CD Configuration

GitHub Actions:

buildcli project add pipeline github

GitLab:

buildcli project add pipeline gitlab

Jenkins:

buildcli project add pipeline jenkins

Set the Active Environment

buildcli p set env dev

This workflow is documented as storing the selected environment in "environment.config".

Build and Run a Docker Container

buildcli project run docker

Review the generated configuration and Docker output before using the resulting image in a production environment.

AI-Powered Documentation

BuildCLI includes a beta feature for generating documentation and comments for Java source code using a locally accessible "Ollama" (https://ollama.com/) service.

Requirements

- Ollama installed and running.
- A locally available model compatible with your Ollama installation.
- Network access from BuildCLI to the local Ollama API.

Start a Model

For example:

ollama run llama3.2

Ensure that the model is available before invoking the documentation workflow.

Document a Java File

buildcli ai code document File.java

The documented workflow sends the specified source file to the local AI service and saves the generated documentation back to the file's original location.

Best practices

- Commit or back up your work before running automated code modifications.
- Review generated comments for technical accuracy.
- Confirm that the documentation reflects actual implementation behavior.
- Avoid sending confidential source code to services that are not approved for your environment.

AI-generated documentation should be treated as a draft requiring developer review.

Configuration Profiles

Configuration profiles allow applications to use different settings for different environments.

Create a Development Profile

buildcli project add profile dev

A profile may correspond to a file such as:

application-dev.properties

Select the Active Environment

buildcli p set env dev

The documented workflow stores the selected profile in:

environment.config

Check the application's execution configuration to confirm how the selected profile is applied in your installed version.

Recommended Profile Organization

A Java application might use the following configuration files:

src/
└── main/
    └── resources/
        ├── application.properties
        ├── application-dev.properties
        └── application-test.properties

This is an illustrative Spring-style layout; the exact files generated by BuildCLI depend on its implementation.

Security recommendation: Never commit passwords, private keys, access tokens, or production credentials. Use environment variables or an appropriate secret-management system.

Docker Integration

BuildCLI provides workflows for generating Dockerfiles and building and running Java application containers.

Generate a Dockerfile

buildcli p add dockerfile

Inspect the generated file to understand its base image, build process, exposed ports, and startup command.

Build and Run a Container

buildcli project run docker

The documented workflow builds a Docker image and runs the application inside a container.

Requirements

- Docker installed and available in your terminal.
- A running Docker daemon.
- A valid Java project and the required build files.
- Any configuration required by the generated Dockerfile.

Production Recommendations

- Prefer maintained, trusted base images.
- Avoid embedding secrets in Docker images.
- Use a non-root application user where practical.
- Expose only required ports.
- Review image size and dependency versions.
- Configure health checks and resource limits where appropriate.
- Test the image before deployment.

Docker Compose

BuildCLI includes functionality for generating Docker Compose configuration files.

Generate a Compose File

buildcli project add dockerCompose

The documented alias is:

buildcli p add dc

The generated "docker-compose.yml" is intended to define a primary service for the Java application using the generated Docker image.

Configure Ports, Volumes, and Resources

The project documentation also describes the following example:

buildcli project add docker-compose --ports 8080:8080 --volumes /data:/app/data --cpu 2 --memory 512m --dockerfile /path/to/Dockerfile

A short-option example is:

buildcli p add dc -p 8080:8080 -v /data:/app/data -c 2 -m 512m -d /path/to/Dockerfile

Version note: The command names and flags above come from the supplied project documentation. Verify the accepted spelling and option names with "buildcli help" before using them, particularly because the documented examples use both "dockerCompose" and "docker-compose".

Start the Containers

buildcli run orchestration up

The documented alias is:

buildcli run oc up

To request a rebuild:

buildcli run orchestration up --build

Or:

buildcli run oc up -b

Stop the Containers

buildcli run orchestration down

Or:

buildcli run oc down

Stop a Named Container

The supplied documentation also lists:

buildcli orchestration down --name <container_name>

Or:

buildcli oc down -n <container_name>

Confirm the exact command hierarchy supported by your installed release before running orchestration operations.

CI/CD Integration

BuildCLI can generate configuration files for common continuous integration and continuous delivery platforms.

GitHub Actions

buildcli project add pipeline github

GitLab CI/CD

buildcli project add pipeline gitlab

Jenkins

buildcli project add pipeline jenkins

These workflows generate configuration files for the selected platform.

Generating a pipeline file does not automatically configure repository permissions, secrets, deployment credentials, runners, agents, or external infrastructure.

Recommended Pipeline Stages

A typical Java CI/CD workflow may include:

1. Check out the source code.
2. Set up the required Java version.
3. Resolve Maven dependencies.
4. Compile the application.
5. Execute automated tests.
6. Package the application.
7. Build and scan a Docker image, if applicable.
8. Deploy through an appropriately protected environment.

The stages above are recommendations and are not a guarantee of the exact contents generated by BuildCLI.

Changelog Generation

BuildCLI provides a changelog generator that analyzes Git commit history and creates structured release notes.

Supported output formats documented by the project include:

- Markdown
- HTML
- JSON

The command is available through "changelog" and the "cl" alias.

Generate a Changelog

buildcli changelog

Or:

buildcli cl

Specify a Version and Output Format

buildcli changelog --version v1.0.0 --format markdown --output CHANGELOG.md

The short-option equivalent is:

buildcli changelog -v v1.0.0 -f markdown -o CHANGELOG.md

Available Options

Option| Short form| Description
"--version"| "-v"| Specifies the release version.
"--format"| "-f"| Selects "markdown", "html", or "json".
"--output"| "-o"| Specifies the output filename.
"--include"| "-i"| Filters commit types using a comma-separated list.

According to the supplied documentation, BuildCLI attempts to detect the latest Git tag when no version is specified. If no tag is found, it uses "Unreleased".

Filter Commit Types

Generate a changelog containing feature and bug-fix commits:

buildcli changelog --version v1.0.0 --format markdown --include feat,fix --output CHANGELOG.md

Supported examples of commit types include:

- "feat"
- "fix"
- "docs"
- "refactor"

Generate HTML or JSON

HTML:

buildcli changelog --version v1.0.0 --format html --output CHANGELOG.html

JSON:

buildcli changelog --version v1.0.0 --format json --output CHANGELOG.json

Changelog Best Practices

- Run the command inside the intended Git repository.
- Ensure the relevant commit history is available.
- Create and maintain release tags.
- Use consistent commit message conventions.
- Review generated release notes before publishing.

Global Git Ignore Configuration

A global Git ignore file allows you to exclude personal editor files, operating-system metadata, temporary files, and local development artifacts across all your repositories.

Unlike a repository-level ".gitignore", a global ignore file applies to repositories for the current user account.

Important: Global ignore rules should be used for personal and machine-specific files. Project-specific build outputs and files that every contributor should ignore belong in the repository's ".gitignore".

1. Create a Global Ignore File

On Linux and macOS:

mkdir -p ~/.config/git
touch ~/.config/git/ignore

2. Configure Git

git config --global core.excludesfile ~/.config/git/ignore

3. Add Common Ignore Rules

Open "~/.config/git/ignore" and add:

# Operating system files
.DS_Store
Thumbs.db
Desktop.ini

# IDEs and editors
.idea/
.vscode/
*.swp
*.swo
*~

# Temporary files
*.tmp
*.temp
*.bak
*.log

# Local environment files
.env
.env.*
!.env.example

# Personal development artifacts
.DS_Store?

Customize these rules to match your workflow. Do not globally ignore files that the project intentionally tracks or that collaborators need.

Windows

You can create a global ignore file in your user profile and configure Git to use it:

git config --global core.excludesfile "%USERPROFILE%\.gitignore_global"

Create "%USERPROFILE%\.gitignore_global" and add the ignore patterns you want to apply across your repositories.

4. Verify the Configuration

Display the configured path:

git config --global --get core.excludesfile

Check whether Git ignores a file:

git check-ignore -v path/to/file

Important Git Behavior

Ignore rules do not automatically remove files that are already tracked by Git.

To stop tracking a file while keeping it on disk, use:

git rm --cached path/to/file

Review the staged changes before committing, particularly when working with configuration files or sensitive data.

For a Java and Maven project, keep shared rules such as "target/" and project-specific generated artifacts in the repository's own ".gitignore", unless the project has a specific reason to handle them differently.

Security Best Practices

Security should be considered throughout the development lifecycle.

- Secrets: Keep credentials and tokens out of source control.
- Dependencies: Use explicit versions where appropriate and review dependency updates.
- AI documentation: Review generated modifications before committing.
- Containers: Avoid embedding credentials in Docker images or build arguments.
- CI/CD: Store deployment credentials in the platform's protected secret store.
- Configuration: Separate development and production settings.
- Git ignore: Remember that ignore rules are not a security boundary and do not remove files already tracked by Git.

These recommendations complement BuildCLI's development workflows; they do not imply that every security control is implemented automatically by the CLI.

Troubleshooting

The "buildcli" Command Is Not Recognized

- Verify that the launcher exists.
- Check that the installation directory is included in "PATH".
- Confirm that the launcher can locate "buildcli.jar".
- Open a new terminal after modifying environment variables.

Java or Maven Errors

Check the installed versions:

java -version
mvn -version

Then verify that the project contains a valid "pom.xml" and review Maven's output for dependency-resolution or compilation errors.

Docker Commands Fail

- Confirm that Docker is installed.
- Verify that the Docker daemon is running.
- Check the current user's permissions.
- Review the generated Dockerfile and container logs.
- Confirm that required ports and files are accessible.

AI Documentation Does Not Work

- Verify that Ollama is installed and running.
- Confirm that the requested model is available locally.
- Check connectivity to the Ollama API.
- Review the installed BuildCLI version's help output.

A Command or Option Is Not Available

BuildCLI has undergone architectural changes, and command syntax may differ between versions.

Run:

buildcli help

Also check the "repository" (https://github.com/BuildCLI/BuildCLI), "issue tracker" (https://github.com/BuildCLI/BuildCLI/issues), and "release notes" (https://github.com/BuildCLI/BuildCLI/releases).

When reporting a bug, include the BuildCLI version, operating system, command used, and relevant error output. Remove secrets and sensitive paths before sharing logs publicly.

Contributing

Contributions are welcome! You can help improve BuildCLI by reporting bugs, suggesting features, improving documentation, or submitting code changes.

Contribution Workflow

1. Fork the "BuildCLI repository" (https://github.com/BuildCLI/BuildCLI).
2. Clone your fork.
3. Create a focused feature branch.
4. Implement your changes.
5. Run the relevant tests and checks.
6. Update documentation when behavior or commands change.
7. Commit and push your changes.
8. Open a pull request against the appropriate upstream branch.

Example Workflow

Create a feature branch:

git checkout -b feature/my-feature

Stage and commit your changes:

git add .
git commit -m "Add my feature"

Push the branch:

git push origin feature/my-feature

Then open a pull request and describe the motivation, implementation, and validation performed.

Before Submitting

- Keep changes focused and easy to review.
- Follow the existing project conventions.
- Test affected functionality.
- Update relevant documentation.
- Avoid committing generated artifacts, credentials, or unrelated changes.
- Review "CONTRIBUTING.md" if it exists in the repository.

License

BuildCLI is distributed under the "MIT License" (https://opensource.org/licenses/MIT).

See the repository's "LICENSE" file for the complete license terms.

Project Resources

- Repository: https://github.com/BuildCLI/BuildCLI
- Releases: https://github.com/BuildCLI/BuildCLI/releases
- Issues: https://github.com/BuildCLI/BuildCLI/issues
- Project Familiarization Guide: "PROJECT_FAMILIARIZATION.md" (https://github.com/BuildCLI/BuildCLI/blob/master/PROJECT_FAMILIARIZATION.md)
- License: MIT

---

<p align="center">
  <strong>Built by the community, for the community.</strong>
</p><p align="center">
  Making Java project development simpler, more consistent, and easier to automate.
</p>