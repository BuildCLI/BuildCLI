package dev.buildcli.core.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SystemOutFormatter extends Formatter {

  @Override
  public String format(LogRecord logRecord) {
    return logRecord.getMessage();
  }

}
