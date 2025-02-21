package org.buildcli.commands;

import org.buildcli.commands.doctor.FixCommand;
import org.buildcli.commands.doctor.ScanCommand;
import picocli.CommandLine.Command;

@Command(
    name = "doctor",
    description = "Analyzes the environment to ensure all necessary tools and dependencies are available and functioning correctly. Provides subcommands for diagnosing issues and applying fixes, ensuring smooth execution of other commands.",
    mixinStandardHelpOptions = true,
    subcommands = {ScanCommand.class, FixCommand.class}
)
public class DoctorCommand {
}
