package org.buildcli.utils;

import org.buildcli.domain.git.GitCommandExecutor;
import org.buildcli.log.SystemOutLogger;

import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
public class BuildCLIIntro {

	private static final GitCommandExecutor gitExec = new GitCommandExecutor();
	private static final String localRepository = gitExec.findGitRepository(getBuildCLIBuildDirectory());

	public BuildCLIIntro() { }

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
		SystemOutLogger.log("Visit the repository for more details: https://github.com/BuildCLI/BuildCLI\n");


		gitExec.showContributors(localRepository);
	}

	public static void checkUpdates(){
		checkIfBuildCLILocalRepositoryIsUpdated();
	}

	private static void checkIfBuildCLILocalRepositoryIsUpdated() {
		boolean updated = gitExec.checkIfLocalRepositoryIsUpdated(localRepository);
		if (!updated){
			SystemOutLogger.log("""
                    \u001B[33m
                    ATTENTION: Your BuildCLI is outdated!
                    \u001B[0m""");
			//+"Do you wanna update? (Y/N)");
		}
	}

	private static String getBuildCLIBuildDirectory() {
		try (InputStream inputStream = BuildCLIIntro.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
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
