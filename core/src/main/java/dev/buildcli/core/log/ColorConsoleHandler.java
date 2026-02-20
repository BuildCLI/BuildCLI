package dev.buildcli.core.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ColorConsoleHandler extends ConsoleHandler {

  @Override
  public synchronized void publish(LogRecord record) {
    if (record.getLevel() == Level.INFO) {
      record.setMessage("\u001B[32m" + record.getMessage() + "\u001B[0m");
    } else if (record.getLevel() == Level.WARNING) {
      record.setMessage("\u001B[33m" + record.getMessage() + "\u001B[0m");
    } else if (record.getLevel() == Level.SEVERE) {
      record.setMessage("\u001B[31m" + record.getMessage() + "\u001B[0m");
    }

    super.publish(record);
  }
}
