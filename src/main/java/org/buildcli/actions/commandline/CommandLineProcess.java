package org.buildcli.actions.commandline;

import java.util.List;

public interface CommandLineProcess {
  int run();
  List<String> output();
}
