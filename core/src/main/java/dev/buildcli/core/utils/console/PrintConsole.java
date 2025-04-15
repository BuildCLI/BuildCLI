package dev.buildcli.core.utils.console;

public final class PrintConsole {
  public static void print(Object...o) {
    var builder = new StringBuilder();

    for (Object o1 : o) {
      if (!builder.isEmpty()) {
        builder.append(" ");
      }

      builder.append(o1.toString());
    }

    System.out.print(builder);
  }

  public static void println(Object...o) {
    var builder = new StringBuilder();

    for (Object o1 : o) {
      if (!builder.isEmpty()) {
        builder.append(" ");
      }

      builder.append(o1.toString());
    }

    System.out.println(builder);
  }
}
