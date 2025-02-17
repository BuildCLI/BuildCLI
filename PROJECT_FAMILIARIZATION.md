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

**1. Fork the repository**

   First, fork the BuildCLI repository to your own GitHub account by clicking the "Fork" button on the repository page.


**2. Clone your fork**
 
   Clone your forked repository to your local machine using the following command
   git clone https://github.com/your-username/buildcli.git
   Replace your-username with your actual GitHub username.


**3. Create a new branch**
 
   Create a new branch for your changes to keep your work isolated from the main codebase:

   `git checkout -b feature/my-feature`

   Replace my-feature with a descriptive name for the feature or bug fix you're working on.


   **4. Make changes**

   Make the necessary changes in the code. Be sure to follow the existing code style and add tests if applicable.


**5. Commit your changes**

   After making your changes, commit them with a clear and concise commit message. For example:

   `git commit -m "Add feature to improve X functionality"`


**6. Push to your fork**

   Push your changes to your forked repository:

  ` git push origin feature/my-feature`
 

**7. Open a Pull Request (PR)**

   Open a Pull Request to the main BuildCLI repository. In your PR description, explain the changes you’ve made and why they are important.
   Be as detailed as possible so that reviewers can easily understand your changes.


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










