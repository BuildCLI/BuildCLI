package dev.buildcli.core.utils;

import java.util.Optional;
import java.util.function.Supplier;

public class ConditionalRunner {
  public static <T> Optional<T> runOnCondition(boolean condition, Supplier<T> supplier) {
    return condition ? Optional.ofNullable(supplier.get()) : Optional.empty();
  }

  public static void runOnCondition(boolean condition, Runnable runnable) {
    if (condition) {
      runnable.run();
    }
  }
}
