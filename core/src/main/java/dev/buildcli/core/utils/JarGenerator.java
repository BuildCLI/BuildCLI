package dev.buildcli.core.utils;

import dev.buildcli.core.actions.commandline.CommandLineProcess;
import dev.buildcli.core.actions.commandline.MavenProcess;

import java.io.File;

public class JarGenerator {
    public static void generateJar(String buildCLIDirectory){
        OS.cdDirectory("");
        OS.cdDirectory(buildCLIDirectory);

        CommandLineProcess process = MavenProcess.createPackageProcessor(new File("."));

        var exitedCode = process.run();

        if (exitedCode == 0) {
            System.out.println("Success...");
        } else {
            System.out.println("Failure...");
        }
    }
}
