# Introduction

   BuildCLI is a powerful command-line interface (CLI) tool aimed at simplifying Java project management. 
   It automates a wide range of development tasks, allowing developers to initialize projects, compile code, manage dependencies, run applications, and integrate CI/CD workflows directly from the terminal.

With BuildCLI, developers can:

* Quickly initialize a new project with a predefined structure.
   

* Compile and build Java applications using Maven.
 

* Easily manage project dependencies, adding or removing libraries in the `pom.xml`.


* Generate documentation for Java files using AI-powered tools.


* Dockerize projects with auto-generated Dockerfiles for easy containerization.


* Integrate CI/CD pipelines to automate deployment and testing workflows across platforms like GitHub Actions, Jenkins, and GitLab.


* This tool is designed to streamline the Java development lifecycle, making it easier for developers to focus on writing code while automating repetitive tasks and ensuring smooth project management.

## PicoCLI and Annotations

**PicoCLI** is a powerful library used for building command-line applications in Java. In BuildCLI, PicoCLI is used to define commands and subcommands, enhancing the modularity and flexibility of the application.

**Annotations** play a crucial role in defining and organizing commands in BuildCLI. For example:
- The `@Command` annotation is used to define a command, such as `ProjectCommand`, `RmCommand`, and `AddCommand`.
- These annotations help in managing the commands, specifying options, and linking them to their corresponding actions in a structured manner.

By using PicoCLI's annotations, BuildCLI achieves an organized and maintainable way to define complex command-line operations.



# Project Structure

   The BuildCLI project follows a modular structure to keep the code organized and maintainable. 
   Below is a brief overview of the main directories and files in the project:

   * **src/:** Contains all source code files for the project.

   * **main/java/:** This is where the core functionality of the CLI tool resides. It contains all Java classes that implement the different commands and features of BuildCLI.

   * **resources/:** Contains configuration files and resources used by the application.

   * **target/:** The directory where the compiled artifacts (such as JAR files) are stored after building the project with Maven.

   * **docs/:** This folder holds all documentation files, including this one.

   * **pom.xml:** The Maven Project Object Model (POM) file that defines the project's dependencies, build configurations, and plugins.

   * **README.md:** Provides an overview of the project, setup instructions, and basic usage.

   * **CONTRIBUTING.md:** Contains guidelines for contributing to the project, including how to report issues and submit pull requests.

      **This structure helps in organizing the different components of the tool, making it easy to navigate and extend as the project grows.**


## Classes with Interfaces

In BuildCLI, several key classes implement interfaces, allowing for modularity and flexibility in the application. For instance, the `RunCommand` class implements the `BuildCLICommand` interface.

- The `RunCommand` class is responsible for executing the main build command, orchestrating other components to run the build process.
- The interface `BuildCLICommand` ensures that all command classes have a consistent structure, making it easier to extend and maintain the project.

By using interfaces, BuildCLI can easily add or replace commands without affecting the rest of the codebase, promoting scalability and adaptability.


## Color System for Logs

BuildCLI utilizes a color-coding system for logs to improve the clarity and readability of messages in the terminal.

- **Info** messages are displayed in **blue**, providing general information about the build process.
- **Warning** messages appear in **yellow**, drawing attention to potential issues or cautions.
- **Error** messages are highlighted in **red**, indicating critical problems that need immediate attention.

This color system makes it easier for developers to quickly identify different types of log messages, ensuring better troubleshooting and faster responses to issues.








# Key Classes and Their Purposes

   Here are some of the key classes in BuildCLI and their roles:
   
*    **VersionCommand:** This class handles the version command, which displays the current version of BuildCLI. It's responsible for showing version information to the user when they run the buildcli --version command.


*    **CommandLineRunner:** This is the entry point for the application, where the command-line interface is initialized and commands are parsed and executed. It ties together all the functionality provided by BuildCLI.


*    **BuildCLIIntro:** This class provides introductory messages and updates when the user first runs BuildCLI. It may include information about new features, changelogs, or general usage tips.


*    **ProjectInitializer:** This class is responsible for initializing a new project, setting up the necessary directories and files (e.g., src/main/java, `pom.xml`, and README.md). It helps users quickly start a new Java project using BuildCLI.


*    **DependencyManager:** This class manages the addition and removal of dependencies in the project's `pom.xml`. It provides the functionality to add new libraries or update existing ones with specified versions.


*    **Dockerizer:** This class generates the Dockerfile for the project, enabling the containerization of the application. It simplifies the process of creating a Docker image for the Java project.


*    **CICDIntegration:** This class is responsible for setting up CI/CD pipelines by generating configuration files for Jenkins, GitHub Actions, and GitLab. It automates the setup of continuous integration and deployment workflows.

     **Each of these classes plays a crucial role in the functionality of BuildCLI, ensuring that developers have the necessary tools to manage their Java projects efficiently from the command line.**


## AI Functionalities

BuildCLI leverages artificial intelligence to automate repetitive tasks and enhance productivity. One of the key AI-powered features is the **automated documentation generation**.

- The `DocumentCommand` utilizes AI to generate documentation for the project, reducing the time spent on manual documentation and ensuring consistency across the codebase.
- This feature automatically analyzes the code and generates relevant documentation, making it easier for developers to keep the documentation up to date.

These AI features are designed to help developers focus on core tasks while automating time-consuming processes, thus enhancing overall efficiency.



   # Navigating Commands and Subcommands

   BuildCLI provides a variety of commands for managing your Java projects. Below is an overview of the most important commands and how to use them.
   For more detailed help, you can always run:
   ```bash
      buildcli help
   ```
      
      
**1. Initialize a New Project**

   This command initializes a new Java project with the required directory structure.

   To initialize a project with a specific name, use the following command:
   ```bash
       buildcli project init MyProject
   ```    
 
   This will create the project structure with MyProject as the base package name.
   If you don’t specify a project name, the default package name buildcli will be used:

   ```bash
      buildcli project init
   ```   


   This will create the project structure with buildcli as the base package name.

**2. Compile the Project**

   ```bash
      project build --compile
   ```   


   This command compiles the Java project using Maven.

   To compile the project, simply run:
   
   ```bash
      buildcli project build --compile
   ```   

**3. Add a Dependency to pom.xml**


   This command adds a dependency to the project’s `pom.xml`.

   * To add a dependency with the latest version, use:

   ```bash
      buildcli project add dependency org.springframework:spring-core
   ```   

   * To add a dependency with a specific version, use:

   ```bash
      buildcli project add dependency org.springframework:spring-core:5.3.21
   ```   

**4. Run the Project**

   This command runs the Java project using Spring Boot.

   To run the project, simply execute:
   ```bash
      buildcli project run
   ```   

**5. Generate Dockerfile for the Project**

   This command generates a Dockerfile for your project, enabling easy containerization.
  
   To generate the Dockerfile, run:

   ```bash
      buildcli project add dockerfile
   ```   


**6. Set Up CI/CD Integration**

   This command generates CI/CD pipeline configuration files for GitHub Actions, Jenkins, or GitLab.
   
   For GitHub Actions, use:

   ```bash
      buildcli project add pipeline github
   ```   
   For Jenkins, use:

   ```bash
    buildcli project add pipeline jenkins
   ``` 

# Contributing to the Project

   We welcome contributions to BuildCLI! If you'd like to help improve the project, follow the steps below to get started.

   **Steps to Contribute:**

  please refer to the detailed contribution guidelines in the **[CONTRIBUTING.md](.github/CONTRIBUTING.md)** file. It includes important steps for contributing, including how to fork the project, create branches, commit your changes, and submit pull requests.



**Code Style Guidelines**
* Follow the Java code style conventions used in the project.
* Ensure that your code is well-commented and properly formatted.
* If your changes are significant, consider writing additional documentation or updating existing docs.


# Tests
* If you are adding or modifying functionality, please add corresponding tests.
* Make sure all existing tests pass after your changes by running the test suite.


# Reporting Issues

   If you find a bug or would like to suggest a new feature, feel free to open an issue in the repository. 
   When opening an issue, please follow the template provided and provide as much detail as possible to help the maintainers understand and fix the problem.


# Code of Conduct

   We ask that all contributors follow the project's Code of Conduct to ensure a welcoming and respectful environment for everyone involved.


# Conclusion

   Thank you for taking the time to explore BuildCLI! We hope that this documentation helps you understand the project's goals, structure, and how to use and contribute to it.
   Whether you're just getting started or you're already familiar with the project, your contributions are always welcome.
   If you have any questions, need help, or want to share your ideas, feel free to reach out or open an issue on GitHub. 
   Together, we can continue to improve and expand BuildCLI for the Java development community!










