package dev.buildcli.cli.commands;

import dev.buildcli.cli.commands.ai.CodeCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ai", description = "Command to use ai features", mixinStandardHelpOptions = true,
    subcommands = {CodeCommand.class}
)
public class AiCommand {
  @Option(names = {"--model", "-m"}, description = "LLM model. If absent, properties file will be used.")
  private String model;

  @Option(names = {"--vendor", "-v"}, description = "LLM vendor. If absent, properties file will be used.")
  private String vendor;

  @Option(names = {"--host", "-H"}, description = "LLM host. If absent, properties file will be used.")
  private String host;

  @Option(names = {"--user", "-u"}, description = "LLM user. If absent, properties file will be used.")
  private String user;

  @Option(names = {"--password", "--pwd", "-p"}, description = "LLM password. If absent, properties file will be used.", interactive = true)
  private String password;

  @Option(names = {"--token", "-t"}, description = "LLM token. If absent, properties file will be used.")
  private String token;

  public String getModel() {
    return model;
  }

  public String getVendor() {
    return vendor;
  }

  public String getHost() {
    return host;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public String getToken() {
    return token;
  }
}
