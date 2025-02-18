package org.buildcli.utils;

import org.buildcli.commands.project.BuildCommand;
import org.buildcli.domain.git.GitCommandExecutor;
import org.buildcli.log.SystemOutLogger;

import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.buildcli.utils.tools.CLIInteractions;

/*
*
*
,-----.          ,--.,--.   ,--. ,-----.,--.   ,--.
|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |
|  .-.  \|  ||  |,--.|  |' .-. ||  |    |  |   |  |
|  '--' /'  ''  '|  ||  |\ `-' |'  '--'\|  '--.|  |
`------'  `----' `--'`--' `---'  `-----'`-----'`--'

*
* */
public class BuildCLIService {

	private static final GitCommandExecutor gitExec = new GitCommandExecutor();
	private static final BuildCommand build =new BuildCommand();

	private static final String buildCLIDirectory =getBuildCLIBuildDirectory();
	private static final String localRepository = gitExec.findGitRepository(buildCLIDirectory);

	public BuildCLIService() { }

	public static void welcome(){
		System.out.println(",-----.          ,--.,--.   ,--. ,-----.,--.   ,--.");
		System.out.println("|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |");
		System.out.println("|  .-.  \\|  ||  |,--.|  |' .-. ||  |    |  |   |  |       Built by the community, for the community");
		System.out.println("|  '--' /'  ''  '|  ||  |\\ `-' |'  '--'\\|  '--.|  |");
		System.out.println("`------'  `----' `--'`--' `---'  `-----'`-----'`--'");
		System.out.println();
	}

	public static void about() {
		SystemOutLogger.log("BuildCLI is a command-line interface (CLI) tool for managing and automating common tasks in Java project development.\n" +
				"It allows you to create, compile, manage dependencies, and run Java projects directly from the terminal, simplifying the development process.\n");
		SystemOutLogger.log("Visit the repository for more details: https://github.com/wheslleyrimar/BuildCLI\n");

		gitExec.showContributors(localRepository, "https://github.com/BuildCLI/BuildCLI.git");
	}

	private static void updateBuildCLI() {
		if (updateRepository()) {
			generateBuildCLIJar();
			String homeBuildCLI = OS.getHomeBinDirectory();
			OS.cpDirectoryOrFile(buildCLIDirectory + "/target/buildcli.jar", homeBuildCLI);
			OS.chmodX(homeBuildCLI + "/buildcli.jar");
			SystemOutLogger.log("\u001B[32mBuildCLI updated successfully!\u001B[0m");
		}else {
			SystemOutLogger.log("\u001B[33mBuildCLI update canceled!\u001B[0m");
		}
	}

	public static void checkUpdatesBuildCLIAndUpdate() {
		boolean updated = gitExec.checkIfLocalRepositoryIsUpdated(localRepository, "https://github.com/BuildCLI/BuildCLI.git");
		if (!updated){
			SystemOutLogger.log("""
                    \u001B[33m
                    ATTENTION: Your BuildCLI is outdated!
                    \u001B[0m""");
			updateBuildCLI();
		}
	}

	private static boolean updateRepository(){
		if(CLIInteractions.getConfirmation("update BuildCLI")){
			gitExec.updateLocalRepositoryFromUpstream(localRepository,"https://github.com/BuildCLI/BuildCLI.git");
			return true;
		}
		return false;
	}

	private static void generateBuildCLIJar(){
		OS.cdDirectory("");
		OS.cdDirectory(buildCLIDirectory);
		build.run();
	}

	private static String getBuildCLIBuildDirectory() {
		try (InputStream inputStream = BuildCLIService.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
			if (inputStream == null) {
				throw new IllegalStateException("Manifest not found.");
			}
			Manifest manifest = new Manifest(inputStream);
			Attributes attributes = manifest.getMainAttributes();
			String buildDirectory = attributes.getValue("Build-Directory");

			if (buildDirectory == null) {
				throw new IllegalStateException("Build-Directory not found in the Manifest.");
			}

			return buildDirectory;
		} catch (Exception e) {
			throw new RuntimeException("Error reading the Manifest.", e);
		}
	}

}
