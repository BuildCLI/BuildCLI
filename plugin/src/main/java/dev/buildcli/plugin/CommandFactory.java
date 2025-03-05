package dev.buildcli.plugin;

import picocli.CommandLine;
import picocli.CommandLine.Command;

public class CommandFactory {
 public static CommandLine createCommandLine(BuildCLICommandPlugin plugin) {
   var parents = plugin.parents();
   var name = plugin.getClass().getDeclaredAnnotation(Command.class).name();

   if (parents == null || parents.length == 0) {
     return new CommandLine(plugin);
   }

   var spec = CommandLine.Model.CommandSpec.create().name(parents[0]);
   var current = spec;

   for (var i = 1; i < parents.length; i++) {
     var command = CommandLine.Model.CommandSpec.create().name(parents[i]);
     command.mixinStandardHelpOptions(true);
     current.addSubcommand(command.name(), command);

     current = command;
   }

   current.addSubcommand(name, new CommandLine(plugin));

   return new CommandLine(spec);
 }

}
