package org.buildcli;

import org.buildcli.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

@Command(name = "BuildCLI", mixinStandardHelpOptions = true,
        versionProvider = BuildCLI.VersionProvider.class,
        description = "BuildCLI - A CLI for Java Project Management",
        subcommands = {
                AutocompleteCommand.class, ProjectCommand.class, VersionCommand.class,
                AboutCommand.class, CommandLine.HelpCommand.class, ConfigCommand.class
        }
)
public class BuildCLI {

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {

            FileReader reader = new FileReader("pom.xml");
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model model = mavenReader.read(reader);
            String version = model.getVersion();

            System.out.println("Retrieved version from pom.xml: " + version);


            if (version != null) {
                return new String[] { "BuildCLI " + version };
            } else {
                return new String[] { "BuildCLI unknown version" };
            }
        }
    }
}
