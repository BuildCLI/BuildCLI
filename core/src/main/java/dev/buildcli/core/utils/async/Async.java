package dev.buildcli.core.utils.async;

import dev.buildcli.core.utils.threads.ThreadPoolUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Async<T> {
  private final CompletableFuture<T> execsAsync;

  public Async(Supplier<T> supplier) {
    this.execsAsync = CompletableFuture.supplyAsync(supplier, ThreadPoolUtils.virtual());
  }

  public boolean isDone() {
    return execsAsync.isDone();
  }

  public T await() throws InterruptedException {
    return execsAsync.join();
  }

  public <R> Async<R> then(Function<? super T, ? extends R> function) {
    return new Async<>(() -> function.apply(execsAsync.join()));
  }

  public Async<Void> consumeAsync(Consumer<? super T> consumer) {
    return new Async<>(() -> execsAsync.thenAccept(consumer).join());
  }

  public static <T> Async<T> run(Supplier<T> supplier) {
    return new Async<>(supplier);
  }

  public static Async<Void> justRun(Runnable runnable) {
    return new Async<>(() -> {
      runnable.run();
      return null;
    });
  }

  public static Async[] group(int size) {
    return new Async[size];
  }

  public static void awaitAll(Async... asyncs) {
    var completableFutures = new CompletableFuture[asyncs.length];

    for (int i = 0; i < asyncs.length; i++) {
      completableFutures[i] = asyncs[i].execsAsync;
    }

    CompletableFuture.allOf(completableFutures).join();
  }

  public Async<T> catchAny(Function<Throwable, T> throwableVoidFunction) {

    execsAsync.exceptionally(throwableVoidFunction);

    return this;
  }
}
