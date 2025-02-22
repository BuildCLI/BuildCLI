package dev.buildcli.core.actions.commandline;

import java.util.List;

public interface CommandLineProcess {
  int run();
  List<String> output();
}
