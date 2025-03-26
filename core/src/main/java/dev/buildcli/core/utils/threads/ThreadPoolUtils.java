package dev.buildcli.core.utils.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ThreadPoolUtils {
  private static ExecutorService fixedInstance;
  private static ExecutorService virtualInstance;

  private ThreadPoolUtils() {}

  public static ExecutorService fixed(int numThreads) {
    if (numThreads <= 0 && fixedInstance == null) {
      throw new IllegalArgumentException("numThreads must be greater than 0");
    }

    if (fixedInstance == null) {
      fixedInstance = Executors.newFixedThreadPool(Math.min(numThreads, Runtime.getRuntime().availableProcessors() - 1));
    }

    return fixedInstance;
  }

  public static ExecutorService virtual() {
    if (virtualInstance == null) {
      virtualInstance = Executors.newVirtualThreadPerTaskExecutor();
    }

    return virtualInstance;
  }

}
